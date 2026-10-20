(ns couplea.order
  "Custom Orders: a form that posts a neat embed to the in-character Discord
   channel through a webhook."
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [couplea.data :as d]
            [couplea.ui :as ui]))

;; ---------------------------------------------------------------- webhook
;;
;; The webhook URL is filled in at BUILD time from the GitHub Actions secret
;; DISCORD_WEBHOOK_URL (see shadow-cljs.edn and .github/workflows/deploy.yml).
;; It is never committed to the repo.
;;
;; HEADS UP: because this is a website with no server, the URL still ends up
;; inside the published JavaScript. Someone determined could dig it out and
;; post to the channel. That is why there is a honeypot, a cooldown, no pings,
;; and why the webhook should point at a channel you don't mind spam in. If it
;; ever leaks, delete the webhook in Discord and make a new one.
;;
;; If no URL is set (empty string), the form runs in TEST MODE: it logs the
;; payload to the browser console and shows the success message.

(goog-define WEBHOOK_URL "")

(def ^:private webhook-url (str/trim WEBHOOK_URL))

(def ^:private cooldown-ms (* 3 60 1000))
(def ^:private cooldown-key "couplea:last-order")
(def ^:private embed-color 0xd6c1a8)

(def limits
  {:name 40 :phone 16 :what 800 :budget 40 :needed-by 60})

(def makers
  [["buck" "Buck"] ["hunter" "Hunter"] ["either" "Either"]])

(def order-categories
  (conj (mapv (fn [{:keys [id label]}] [(name id) label]) d/categories)
        ["other" "Something else"]))

;; ---------------------------------------------------------------- cooldown

(defonce ^:private last-sent-in-memory (atom nil))

(defn- stored-time []
  (try
    (when-let [v (.getItem js/localStorage cooldown-key)]
      (let [n (js/parseInt v 10)]
        (when-not (js/isNaN n) n)))
    (catch :default _ nil)))

(defn- remember-sent! []
  (let [now (.now js/Date)]
    (reset! last-sent-in-memory now)
    (try (.setItem js/localStorage cooldown-key (str now))
         (catch :default _ nil))))

(defn- cooldown-left
  "Milliseconds left before another order may be sent, or nil."
  []
  (when-let [t (max (or (stored-time) 0) (or @last-sent-in-memory 0))]
    (let [left (- (+ t cooldown-ms) (.now js/Date))]
      (when (and (pos? t) (pos? left)) left))))

;; ---------------------------------------------------------------- payload

(defn normalize-phone
  "Strips spaces, dashes, dots and brackets."
  [s]
  (str/replace (str s) (js/RegExp. "[\\s\\-.()]" "g") ""))

(defn- validate [v]
  (cond-> {}
    (str/blank? (:name v))
    (assoc :name "Tell us your name.")

    (not (re-matches #"\d{3,12}" (normalize-phone (:phone v))))
    (assoc :phone "Digits only, please. Like 285538.")

    (str/blank? (:maker v))
    (assoc :maker "Pick who you'd like to make it.")

    (str/blank? (:category v))
    (assoc :category "Pick a category.")

    (str/blank? (:what v))
    (assoc :what "Tell us what you're after.")

    (str/blank? (:needed-by v))
    (assoc :needed-by "When do you need it by?")))

(defn- tidy
  "Trim, and stop @mentions from looking like pings in the channel."
  [s]
  (str/replace (str/trim (str s)) "@" "@​"))

(defn- label-for [pairs k]
  (some (fn [[id label]] (when (= id k) label)) pairs))

(defn build-payload
  "The JSON body for Discord, as a Clojure map. It is turned into a JS object
   with clj->js right before sending so the keys are never renamed."
  [v role-id]
  (let [budget (tidy (:budget v))
        field  (fn [n val inline?] {:name n :value val :inline inline?})]
    (cond-> {:allowed_mentions (cond-> {:parse []}
                                 role-id (assoc :roles [role-id]))
             :embeds
             [{:title     "New Custom Order"
               :color     embed-color
               :timestamp (.toISOString (js/Date.))
               :fields    [(field "Name" (tidy (:name v)) true)
                           (field "Phone" (normalize-phone (:phone v)) true)
                           (field "Wants" (label-for makers (:maker v)) true)
                           (field "Category" (label-for order-categories (:category v)) true)
                           (field "Needed by" (tidy (:needed-by v)) true)
                           (field "Budget" (if (str/blank? budget) "Not given" budget) true)
                           (field "What they want" (tidy (:what v)) false)]}]}
      role-id (assoc :content (str "<@&" role-id "> New custom order")))))

(defn- post! [payload on-ok on-fail]
  (if (str/blank? webhook-url)
    (do
      (js/console.info "[Couple'a Cryptids] TEST MODE: no Discord webhook is set, so nothing was sent. Here is what would have been posted:")
      (js/console.log (js/JSON.stringify (clj->js payload) nil 2))
      (js/setTimeout on-ok 350))
    (let [ctrl  (js/AbortController.)
          timer (js/setTimeout #(.abort ctrl) 15000)]
      (-> (js/fetch webhook-url
                    (clj->js {:method  "POST"
                              :headers {"Content-Type" "application/json"}
                              :body    (js/JSON.stringify (clj->js payload))
                              :signal  (.-signal ctrl)}))
          (.then (fn [resp]
                   (js/clearTimeout timer)
                   (if (.-ok resp) (on-ok) (on-fail))))
          (.catch (fn [_]
                    (js/clearTimeout timer)
                    (on-fail)))))))

;; ---------------------------------------------------------------- form bits

(def ^:private blank-values
  {:name "" :phone "" :maker "" :category "" :what "" :budget "" :needed-by "" :website ""})

(defn- input-props [{:keys [id errors k]}]
  (cond-> {:id id :name (name k)
           :aria-invalid (boolean (get errors k))}
    (get errors k) (assoc :aria-describedby (str id "-err"))))

(defn- field-shell [{:keys [id label hint optional? error]} control]
  [:div.field {:class (when error "has-error")}
   [:label {:for id}
    label
    (when optional? [:span.opt " (optional)"])]
   control
   (when hint [:p.field__hint hint])
   (when error [:p.field__err {:id (str id "-err")} error])])

(defn- text-field [values errors {:keys [k label hint optional? type input-mode auto-complete]}]
  (let [id (str "order-" (name k))]
    [field-shell {:id id :label label :hint hint :optional? optional? :error (get errors k)}
     [:input (merge (input-props {:id id :errors errors :k k})
                    {:type (or type "text")
                     :value (get @values k)
                     :max-length (limits k)
                     :input-mode input-mode
                     :auto-complete (or auto-complete "off")
                     :on-change #(swap! values assoc k (.. % -target -value))}
                    (when (= k :phone)
                      {:on-blur #(swap! values update :phone normalize-phone)}))]]))

(defn- select-field [values errors {:keys [k label options]}]
  (let [id (str "order-" (name k))]
    [field-shell {:id id :label label :error (get errors k)}
     [:select (merge (input-props {:id id :errors errors :k k})
                     {:value (get @values k)
                      :on-change #(swap! values assoc k (.. % -target -value))})
      [:option {:value ""} "Pick one"]
      (for [[v l] options]
        ^{:key v} [:option {:value v} l])]]))

(defn- focus-first-error!
  "Moves the cursor to the first field with a problem, once it has re-rendered."
  []
  (js/setTimeout
   (fn []
     (when-let [el (.querySelector js/document "#order-form [aria-invalid=true]")]
       (.focus el)))
   60))

;; ---------------------------------------------------------------- the form

(defn order-form []
  (let [values (r/atom blank-values)
        errors (r/atom {})
        status (r/atom :idle) ; :idle :sending :sent :failed :cooling
        wait   (r/atom 0)
        test?  (str/blank? webhook-url)]
    (fn []
      (let [phone (:phone d/site)
            role  (:discord-role-id d/site)
            sending? (= @status :sending)]
        (if (= @status :sent)
          [:div.slip__done {:role "status"}
           [:p.slip__done-title "Got it. We'll give you a ring."]
           (when test?
             [:p.slip__test "Test mode: nothing was actually sent to Discord."])
           [:button.btn.btn--quiet {:type "button"
                                    :on-click #(reset! status :idle)}
            "Write another"]]

          [:form#order-form.slip__form
           {:no-validate true
            :on-submit
            (fn [e]
              (.preventDefault e)
              (let [v @values]
                (cond
                  sending?
                  nil

                  ;; Honeypot: a person can't see this field, a bot fills it in.
                  ;; Pretend it worked and send nothing.
                  (not (str/blank? (:website v)))
                  (do (reset! errors {})
                      (reset! values blank-values)
                      (reset! status :sent))

                  (seq (validate v))
                  (do (reset! errors (validate v))
                      (reset! status :idle)
                      (focus-first-error!))

                  (cooldown-left)
                  (do (reset! errors {})
                      (reset! wait (js/Math.ceil (/ (cooldown-left) 60000)))
                      (reset! status :cooling))

                  :else
                  (do
                    (reset! errors {})
                    (reset! status :sending)
                    (post! (build-payload v (when (seq role) role))
                           (fn []
                             (remember-sent!)
                             (reset! values blank-values)
                             (reset! status :sent))
                           (fn []
                             (reset! status :failed)))))))}

           [text-field values @errors {:k :name :label "Your name" :auto-complete "name"}]
           [text-field values @errors {:k :phone :label "Your phone number"
                                       :hint "Digits only. We'll ring this number."
                                       :type "tel" :input-mode "numeric" :auto-complete "tel"}]
           [select-field values @errors {:k :maker :label "Who should make it?" :options makers}]
           [select-field values @errors {:k :category :label "What kind of thing?" :options order-categories}]

           (let [id "order-what"
                 n (count (:what @values))]
             [field-shell {:id id :label "What do you want?" :error (get @errors :what)
                           :hint (str n " / " (limits :what))}
              [:textarea (merge (input-props {:id id :errors @errors :k :what})
                                {:value (:what @values)
                                 :rows 5
                                 :max-length (limits :what)
                                 :on-change #(swap! values assoc :what (.. % -target -value))})]])

           [text-field values @errors {:k :budget :label "Budget" :optional? true
                                       :hint "A number or a rough idea is fine."}]
           [text-field values @errors {:k :needed-by :label "Needed by"
                                       :hint "A day, or something like \"this weekend\"."}]

           ;; Honeypot. Hidden from people and screen readers, so only bots fill it in.
           [:div.hp {:aria-hidden "true"}
            [:label {:for "order-website"} "Leave this empty"]
            [:input {:id "order-website" :name "website" :type "text"
                     :tab-index -1 :auto-complete "off"
                     :value (:website @values)
                     :on-change #(swap! values assoc :website (.. % -target -value))}]]

           [:div.slip__actions
            [:button.btn {:type "submit" :disabled sending?}
             (if sending? "Sending..." "Send it")]
            [:div.slip__msg {:role "status" :aria-live "polite"}
             (case @status
               :failed
               [:p.msg.msg--bad
                "That didn't go through. Give us a ring at " [:strong phone] " instead."]
               :cooling
               [:p.msg.msg--warn
                "Easy there. Give it about " @wait (if (= @wait 1) " minute" " minutes")
                " before you send another."]
               (when (seq @errors)
                 [:p.msg.msg--warn "A couple of things need fixing above."]))]]])))))

(defn custom-orders []
  [:section.section {:id "custom-orders" :aria-labelledby "orders-title"}
   [:div.wrap.wrap--narrow
    [:header.section-head
     [:h2.section-title.distress {:id "orders-title"} "Custom Orders"]
     [ui/rule]
     [:p.lede "Want something made just for you? Tell us what you're after and who you'd like to make it. We'll give you a ring."]]
    [:div.slip
     [:div.slip__logo {:aria-hidden "true"}]
     [order-form]]]])
