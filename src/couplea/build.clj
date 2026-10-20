(ns couplea.build
  "Build-time helpers (JVM only, never shipped to the browser).

   - check      : validates site.edn + stock.edn, prints plain errors, exit 1 on failure
   - dev-files  : shadow-cljs hook that writes public/index.html + syncs images/
   - finish     : assembles the final site into dist/ after `shadow-cljs release`"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [couplea.validate :as v])
  (:import (java.nio.file Files Path StandardCopyOption)
           (java.security MessageDigest)))

;; ---------------------------------------------------------------- files

(defn- rel-files
  "All regular files under dir, as relative paths with forward slashes."
  [^java.io.File dir]
  (when (.isDirectory dir)
    (let [base (.toPath dir)]
      (->> (file-seq dir)
           (filter #(.isFile ^java.io.File %))
           (map #(str/replace (str (.relativize base (.toPath ^java.io.File %))) "\\" "/"))))))

(defn- skip? [rel]
  (let [fname (last (str/split rel #"/"))]
    (or (str/starts-with? fname ".")
        (str/starts-with? (str/lower-case fname) "readme"))))

(defn- copy-file! [^java.io.File from ^java.io.File to]
  (io/make-parents to)
  (Files/copy (.toPath from) (.toPath to)
              (into-array java.nio.file.CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn- copy-tree! [from-dir to-dir & {:keys [except]}]
  (doseq [rel (rel-files (io/file from-dir))
          :when (and (not (skip? rel))
                     (not (contains? (set except) rel)))]
    (copy-file! (io/file from-dir rel) (io/file to-dir rel))))

(defn- delete-tree! [^java.io.File f]
  (when (.exists f)
    (doseq [x (reverse (file-seq f))] (.delete ^java.io.File x))))

;; ---------------------------------------------------------------- html

(defn- esc [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn render-index
  "Fills {{tokens}} in static/index.html from site.edn."
  [site]
  (let [tokens {"site-url"      (:site-url site)
                "business-name" (:business-name site)
                "title"         (str (:business-name site) " | " (:tagline site))
                "description"   (:description site)
                "phone"         (:phone site)}]
    (reduce (fn [html [k val]]
              (str/replace html (str "{{" k "}}") (esc val)))
            (slurp (io/file "static" "index.html") :encoding "UTF-8")
            tokens)))

;; ---------------------------------------------------------------- tasks

(defn- load-or-explain! []
  (let [{:keys [errors] :as r} (v/load-all)]
    (when (seq errors)
      (throw (ex-info (v/format-errors errors) {:couplea/plain true})))
    r))

(defn check
  "npm run check. Prints a plain report and exits 1 on any mistake."
  [& _]
  (let [{:keys [errors warnings site stock]} (v/load-all)]
    (doseq [w warnings]
      (println (str "NOTE: " w)))
    (if (seq errors)
      (do (binding [*out* *err*] (println (v/format-errors errors)))
          (flush)
          (System/exit 1))
      (do (println (str "OK: site.edn and stock.edn look good. "
                        (count stock) " item(s), "
                        (count (filter :in-stock stock)) " in stock."
                        (when (:business-name site) "")))
          (flush)))))

;; shadow-cljs only watches code files, not .edn files. This tiny watcher
;; "touches" src/couplea/data.cljs whenever stock.edn or site.edn changes, so
;; watch mode recompiles (and re-validates) straight away.
(defonce ^:private data-watcher (atom nil))

(defn- start-data-watcher! []
  (when-not @data-watcher
    (let [dir      (.toPath (.getAbsoluteFile (io/file ".")))
          watcher  (.newWatchService (java.nio.file.FileSystems/getDefault))
          data-ns  (io/file "src" "couplea" "data.cljs")
          watched? #{"stock.edn" "site.edn"}
          worker   (Thread.
                    (fn []
                      (try
                        (loop []
                          (let [k (.take watcher)
                                names (set (map #(str (.context ^java.nio.file.WatchEvent %))
                                                (.pollEvents k)))]
                            (.reset k)
                            (when (some watched? names)
                              (Thread/sleep 250) ; let the save finish
                              (.setLastModified data-ns (System/currentTimeMillis)))
                            (recur)))
                        (catch InterruptedException _ nil))))]
      (.register dir watcher
                 (into-array java.nio.file.WatchEvent$Kind
                             [java.nio.file.StandardWatchEventKinds/ENTRY_MODIFY
                              java.nio.file.StandardWatchEventKinds/ENTRY_CREATE]))
      (.setDaemon worker true)
      (.start worker)
      (reset! data-watcher worker))))

(defn dev-files
  "shadow-cljs build hook (stage :configure). Gives the dev server an
   index.html with the tokens filled in, a copy of images/, and starts the
   stock.edn / site.edn watcher."
  {:shadow.build/stage :configure}
  [build-state & _]
  (let [{:keys [site]} (load-or-explain!)]
    (io/make-parents (io/file "public" "index.html"))
    (spit (io/file "public" "index.html") (render-index site) :encoding "UTF-8")
    (copy-tree! "images" "public/images")
    (start-data-watcher!)
    build-state))

(defn- sha1-8 [^java.io.File f]
  (let [md (MessageDigest/getInstance "SHA-1")
        bs (.digest md (Files/readAllBytes (.toPath f)))]
    (subs (apply str (map #(format "%02x" %) bs)) 0 8)))

(defn finish
  "After `shadow-cljs release app`: build dist/ (the folder that gets published).
   Original art (source-art/) and dev leftovers are never copied."
  [& _]
  (let [{:keys [site]} (load-or-explain!)
        main (io/file "public" "js" "main.js")]
    (when-not (.exists main)
      (throw (ex-info "public/js/main.js is missing. Did the release build run?" {})))
    (delete-tree! (io/file "dist"))
    (copy-tree! "static" "dist" :except ["index.html"])
    (copy-tree! "images" "dist/images")
    (copy-file! main (io/file "dist" "js" "main.js"))
    (spit (io/file "dist" "index.html")
          (str/replace (render-index site)
                       "js/main.js"
                       (str "js/main.js?v=" (sha1-8 main)))
          :encoding "UTF-8")
    (let [n (count (rel-files (io/file "dist")))
          kb (quot (reduce + (map #(.length (io/file "dist" %)) (rel-files (io/file "dist")))) 1024)]
      (println (str "dist/ is ready: " n " files, " kb " KB.")))))
