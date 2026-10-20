(ns couplea.validate
  "Reads and checks site.edn and stock.edn at build time.

   Every problem is returned as a plain-English sentence that names the item
   and what is wrong, so a failed build tells the owners exactly what to fix.
   Runs on the JVM only (never shipped to the browser)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ---------------------------------------------------------------- vocab

(def makers [:buck :hunter :both])
(def categories [:jerky :apples :wood :candles :handmade])

(def stock-keys
  [:name :description :price :category :maker :in-stock :tag :image])

(def required-stock-keys
  [:name :description :price :category :maker :in-stock])

(def stock-hints
  {:name        "a line like   :name \"Smoked Beef Jerky\""
   :description "a line like   :description \"Short description here.\""
   :price       "a line like   :price 300   (or   :price :ask   for \"Ask us\")"
   :category    "a line like   :category :jerky   (one of :jerky :apples :wood :candles :handmade)"
   :maker       "a line like   :maker :hunter   (one of :buck :hunter :both)"
   :in-stock    "a line like   :in-stock true   (or false for SOLD OUT)"})

(def image-extensions #{"webp" "jpg" "jpeg" "png" "gif" "avif"})

;; ---------------------------------------------------------------- helpers

(defn- distance
  "Levenshtein distance, for \"did you mean\" hints."
  [a b]
  (let [a (vec a) b (vec b)]
    (peek
     (reduce (fn [prev i]
               (reduce (fn [row j]
                         (conj row
                               (min (inc (peek row))
                                    (inc (nth prev (inc j)))
                                    (+ (nth prev j)
                                       (if (= (nth a i) (nth b j)) 0 1)))))
                       [(inc i)]
                       (range (count b))))
             (vec (range (inc (count b))))
             (range (count a))))))

(defn- closest [x options]
  (let [s (str/lower-case (str (if (keyword? x) (name x) x)))
        scored (map (fn [o] [(distance s (name o)) o]) options)
        [d best] (first (sort-by first scored))]
    (when (and best (<= d 3)) best)))

(defn- did-you-mean [x options]
  (if-let [c (closest x options)]
    (str " Did you mean " (pr-str c) "?")
    ""))

(defn- show [v] (pr-str v))

(defn- blank-str? [v] (or (nil? v) (and (string? v) (str/blank? v))))

(defn- opts-str [options] (str/join " " (map pr-str options)))

;; ---------------------------------------------------------------- reading

(defn- reader-line [^Exception e]
  (let [d (ex-data e)]
    (or (:line d)
        (:clojure.error/line d)
        (some->> (.getMessage e) (re-find #"[Ll]ine (\d+)") second))))

(defn read-edn
  "Returns {:data x} or {:errors [msg]}. `what` is the file name."
  [^java.io.File f what]
  (if-not (.exists f)
    {:errors [(str what " is missing. It should sit next to package.json.")]}
    (try
      ;; Some Windows editors add an invisible byte-order mark (U+FEFF) at the
      ;; very start of a file. Ignore it.
      (with-open [r (clojure.lang.LineNumberingPushbackReader.
                     (java.io.StringReader.
                      (str/replace (slurp f :encoding "UTF-8") #"^﻿" "")))]
        (let [data  (edn/read {:eof ::eof} r)
              extra (edn/read {:eof ::eof} r)]
          (cond
            (= data ::eof)
            {:errors [(str what " is empty. Put your list back inside it.")]}

            (not= extra ::eof)
            {:errors [(str what " has extra content after the closing bracket. "
                           "Everything must stay inside the outer brackets. "
                           "Look for an item pasted below the last ] or }.")]}

            :else {:data data})))
      (catch Exception e
        (let [line (reader-line e)
              msg  (.getMessage e)
              what-happened
              (cond
                (re-find #"EOF" msg)
                "The file ends before something was closed."
                (re-find #"Unmatched delimiter" msg)
                "There is a closing bracket with nothing to close."
                :else
                (str/replace msg #"^[\w.]+Exception: " ""))]
          {:errors [(str what " has a typo"
                         (when line (str " near line " line))
                         ". " what-happened " "
                         "Usually that's a missing quote \", bracket ] or brace }. "
                         "Check that every opening one has a matching closing one.")]})))))

;; ---------------------------------------------------------------- stock

(defn- item-label [i item]
  (let [n (when (map? item) (:name item))]
    (if (and (string? n) (not (str/blank? n)))
      (str "Item " (inc i) " (\"" n "\")")
      (str "Item " (inc i) " (no name yet)"))))

(defn- image-problem [label image images-dir]
  (cond
    (not (string? image))
    (str label ": :image must be text in quotes, like :image \"images/jerky.webp\". Got " (show image) ".")

    (not (str/starts-with? image "images/"))
    (str label ": :image " (show image) " must start with images/ , like \"images/jerky.webp\".")

    (str/includes? image "..")
    (str label ": :image " (show image) " can't contain \"..\".")

    (not (contains? image-extensions
                    (str/lower-case (last (str/split image #"\.")))))
    (str label ": :image " (show image) " must be a photo ending in "
         (str/join ", " (sort image-extensions)) ".")

    :else
    (let [fname (subs image (count "images/"))
          ok?   (and (.isDirectory ^java.io.File images-dir)
                     (some #(= fname %) (.list ^java.io.File images-dir)))]
      (when-not ok?
        (str label ": the photo " (show image) " isn't in the images folder. "
             "Upload it to the images folder first, then check the spelling "
             "(capital letters matter).")))))

(defn- item-problems [i item images-dir]
  (let [label (item-label i item)]
    (if-not (map? item)
      [(str label " isn't a { } block. Each item must start with { and end with }.")]
      (let [present (set (keys item))
            unknown (remove (set stock-keys) present)
            kw-ok?  (fn [k options]
                      (let [v (get item k)]
                        (cond
                          (not (contains? present k)) nil
                          (contains? (set options) v) nil
                          (and (string? v) (contains? (set options) (keyword v)))
                          (str label ": " k " is " (show v) " in quotes. "
                               "Write it with a colon and no quotes: " (pr-str (keyword v)) ".")
                          :else
                          (str label ": " k " is " (show v) ", which isn't one we know. "
                               "Use one of: " (opts-str options) "."
                               (did-you-mean v options)))))]
        (->> (concat
              ;; unknown field names (catches typos like :instock)
              (for [k unknown]
                (str label " has a field " (show k) " that we don't use."
                     (if (keyword? k)
                       (did-you-mean k stock-keys)
                       " Field names start with a colon, like :name.")
                     (when-not (closest k stock-keys)
                       (str " Allowed fields: " (opts-str stock-keys) "."))))
              ;; missing fields
              (for [k required-stock-keys
                    :when (not (contains? present k))]
                (str label " is missing " k ". Add " (stock-hints k) "."))
              ;; name
              [(when (contains? present :name)
                 (let [v (:name item)]
                   (cond
                     (not (string? v)) (str label ": :name must be text in quotes. Got " (show v) ".")
                     (str/blank? v)    (str label ": :name is blank. Give the item a name.")
                     (> (count v) 60)  (str label ": :name is " (count v) " characters. Keep it under 60."))))]
              ;; description
              [(when (contains? present :description)
                 (let [v (:description item)]
                   (cond
                     (not (string? v)) (str label ": :description must be text in quotes. Got " (show v) ".")
                     (str/blank? v)    (str label ": :description is blank. Write a short line about it.")
                     (> (count v) 240) (str label ": :description is " (count v) " characters. Keep it under 240."))))]
              ;; price
              [(when (contains? present :price)
                 (let [v (:price item)]
                   (cond
                     (= v :ask) nil
                     (string? v)
                     (str label ": :price is " (show v) " in quotes. Write a plain number like :price 300 (or :price :ask).")
                     (and (integer? v) (<= 0 v 100000000)) nil
                     (integer? v)
                     (str label ": :price " v " is out of range. Use whole dollars from 0 up.")
                     (number? v)
                     (str label ": :price " v " has cents or decimals. Use whole dollars, like :price 300.")
                     (keyword? v)
                     (str label ": :price " (show v) " isn't right. The only word allowed is :ask.")
                     :else
                     (str label ": :price must be a whole number like 300, or :ask. Got " (show v) "."))))]
              [(kw-ok? :category categories)]
              [(kw-ok? :maker makers)]
              ;; in-stock
              [(when (contains? present :in-stock)
                 (let [v (:in-stock item)]
                   (when-not (boolean? v)
                     (str label ": :in-stock must be true or false, with no quotes. Got " (show v) "."))))]
              ;; tag (optional)
              [(when (and (contains? present :tag) (not (blank-str? (:tag item))))
                 (let [v (:tag item)]
                   (cond
                     (not (string? v)) (str label ": :tag must be text in quotes. Got " (show v) ".")
                     (> (count v) 24)  (str label ": :tag is " (count v) " characters. Keep the little stamp under 24."))))]
              ;; image (optional)
              [(when (and (contains? present :image) (not (blank-str? (:image item))))
                 (image-problem label (:image item) images-dir))])
             (remove nil?)
             vec)))))

(defn stock-problems
  "Returns a vector of plain-English problems (empty means all good)."
  [data images-dir]
  (cond
    (not (vector? data))
    ["stock.edn must be one list inside square brackets [ ... ] with the items between them."]

    :else
    (let [item-errs (vec (mapcat (fn [i item] (item-problems i item images-dir))
                                 (range) data))
          names (->> data (filter map?) (map :name) (filter string?))
          dupes (->> (frequencies (map str/lower-case names))
                     (filter (fn [[_ n]] (> n 1))) (map first))]
      (into item-errs
            (for [d dupes]
              (str "Two items are both called \"" d "\". Give each item its own name so they're easy to tell apart."))))))

;; ---------------------------------------------------------------- site

(def placeholder-marker "[[")

(defn- placeholder? [v]
  (cond
    (string? v) (or (str/includes? v placeholder-marker)
                    (str/includes? v "YOUR-"))
    (sequential? v) (some placeholder? v)
    (map? v) (some placeholder? (vals v))
    :else false))

(defn- text-problem [path v & {:keys [max] :or {max 400}}]
  (cond
    (not (string? v)) (str "site.edn: " path " must be text in quotes. Got " (show v) ".")
    (str/blank? v)    (str "site.edn: " path " is blank.")
    (> (count v) max) (str "site.edn: " path " is " (count v) " characters. Keep it under " max ".")))

(defn- maker-problems [data k]
  (let [m    (get-in data [:makers k])
        path (str ":makers " k)]
    (if-not (map? m)
      [(str "site.edn: " path " is missing. It needs :name, :role, :bio and :makes.")]
      (remove nil?
              [(text-problem (str path " :name") (:name m) :max 40)
               (text-problem (str path " :role") (:role m) :max 80)
               (text-problem (str path " :bio") (:bio m) :max 700)
               (let [v (:makes m)]
                 (when-not (and (vector? v) (seq v) (every? #(and (string? %) (not (str/blank? %))) v))
                   (str "site.edn: " path " :makes must be a list of names in quotes, like [\"Jerky\" \"Pickles\"].")))]))))

(defn site-problems [data]
  (if-not (map? data)
    ["site.edn must be one { } block of settings."]
    (->> (concat
          (for [k [:business-name :phone :tagline :tagline-short :motto
                   :description :site-url :location :hours :makers]
                :when (not (contains? data k))]
            (str "site.edn is missing " k ". Put it back (copy it from the README or an old version)."))
          [(when (contains? data :business-name) (text-problem ":business-name" (:business-name data) :max 60))
           (when (contains? data :tagline) (text-problem ":tagline" (:tagline data) :max 120))
           (when (contains? data :tagline-short) (text-problem ":tagline-short" (:tagline-short data) :max 120))
           (when (contains? data :motto) (text-problem ":motto" (:motto data) :max 120))
           (when (contains? data :description) (text-problem ":description" (:description data) :max 300))
           (when (contains? data :location) (text-problem ":location" (:location data) :max 300))
           (when (contains? data :phone)
             (let [v (:phone data)]
               (when-not (and (string? v) (re-matches #"\d{3,12}" v))
                 (str "site.edn: :phone must be digits only, in quotes, like \"285538\". Got " (show v) "."))))
           (when (contains? data :site-url)
             (let [v (:site-url data)]
               (when-not (and (string? v) (re-matches #"https?://\S+" v))
                 (str "site.edn: :site-url must start with https:// . Got " (show v) "."))))
           (when (contains? data :hours)
             (let [v (:hours data)]
               (when-not (and (vector? v) (seq v) (every? #(and (string? %) (not (str/blank? %))) v))
                 "site.edn: :hours must be a list of lines in quotes, like [\"Mon to Fri: 6pm to 10pm\" \"Sat: noon to late\"].")))
           (when (contains? data :discord-role-id)
             (let [v (:discord-role-id data)]
               (when-not (or (nil? v)
                             (and (string? v) (re-matches #"\d{17,20}" v))
                             (and (integer? v) (re-matches #"\d{17,20}" (str v))))
                 (str "site.edn: :discord-role-id must be nil (no ping) or the long number of a Discord role, in quotes. Got " (show v) "."))))]
          (when (contains? data :makers)
            (concat (maker-problems data :buck) (maker-problems data :hunter))))
         (remove nil?)
         vec)))

(defn site-warnings [data]
  (when (map? data)
    (cond-> []
      (placeholder? (:site-url data))
      (conj "site.edn :site-url still has the placeholder. Link previews in Discord will point to a page that doesn't exist until you fill it in.")
      (placeholder? (:location data))
      (conj "site.edn :location is still a placeholder.")
      (placeholder? (:hours data))
      (conj "site.edn :hours is still a placeholder.")
      (placeholder? (get-in data [:makers :buck :bio]))
      (conj "site.edn Buck's :bio is still a placeholder.")
      (placeholder? (get-in data [:makers :hunter :bio]))
      (conj "site.edn Hunter's :bio is still a placeholder."))))

;; ---------------------------------------------------------------- loading

(defn- normalize-site [data]
  (cond-> data
    (some? (:discord-role-id data))
    (update :discord-role-id str)
    true
    (update :site-url #(if (str/ends-with? % "/") % (str % "/")))))

(defn load-all
  "Reads and checks both files. Returns
   {:site map :stock vector :errors [..] :warnings [..]}."
  []
  (let [site-r  (read-edn (io/file "site.edn") "site.edn")
        stock-r (read-edn (io/file "stock.edn") "stock.edn")
        site-e  (or (:errors site-r) (site-problems (:data site-r)))
        stock-e (or (:errors stock-r) (stock-problems (:data stock-r) (io/file "images")))
        errors  (vec (concat site-e stock-e))]
    {:errors   errors
     :warnings (when (empty? site-e) (site-warnings (:data site-r)))
     :site     (when (empty? site-e) (normalize-site (:data site-r)))
     :stock    (when (empty? stock-e) (:data stock-r))}))

(defn format-errors
  "One block of text for the terminal / GitHub log."
  [errors]
  (str "\n"
       "==============================================================\n"
       "  BUILD STOPPED: there's a mistake in site.edn or stock.edn\n"
       "  The live site is untouched and keeps its last good version.\n"
       "==============================================================\n"
       (str/join "\n" (map #(str "  * " %) errors))
       "\n==============================================================\n"
       "  Fix the line(s) above, save, and the build will run again.\n"
       "==============================================================\n"))
