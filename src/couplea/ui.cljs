(ns couplea.ui
  "Small pieces used all over the page."
  (:require [reagent.core :as r]
            [couplea.data :as d]
            [couplea.icons :as i]))

;; ---------------------------------------------------------------- copy to clipboard

(defn- copy-with-textarea!
  "Old-school copy for browsers without the clipboard API."
  [text]
  (let [ta (.createElement js/document "textarea")]
    (set! (.-value ta) text)
    (.setAttribute ta "readonly" "")
    (set! (.. ta -style -position) "fixed")
    (set! (.. ta -style -opacity) "0")
    (.appendChild (.-body js/document) ta)
    (.select ta)
    (let [ok (try (.execCommand js/document "copy") (catch :default _ false))]
      (.removeChild (.-body js/document) ta)
      ok)))

(defn copy-text!
  "Copies text, then calls (on-done true|false)."
  [text on-done]
  (let [clip (.-clipboard js/navigator)]
    (if clip
      (-> (.writeText clip text)
          (.then #(on-done true))
          (.catch #(on-done (boolean (copy-with-textarea! text)))))
      (on-done (boolean (copy-with-textarea! text))))))

;; ---------------------------------------------------------------- pieces

(defn phone-button
  "The in-game number. NOT a tel: link (that would try to dial a real phone).
   Tapping copies the number and says \"Copied\"."
  [{:keys [class]}]
  (let [state (r/atom nil)
        timer (atom nil)]
    (fn [{:keys [class]}]
      (let [phone (:phone d/site)]
        [:div.phone {:class class}
         [:button.phone__btn
          {:type "button"
           :aria-label (str "In-game phone number " phone ". Tap to copy.")
           :on-click (fn [_]
                       (copy-text! phone
                                   (fn [ok?]
                                     (reset! state (if ok? :copied :failed))
                                     (js/clearTimeout @timer)
                                     (reset! timer (js/setTimeout #(reset! state nil) 2600)))))}
          [i/phone-badge]
          [:span.phone__bar {:aria-hidden "true"}]
          [:span.phone__num phone]]
         [:span.phone__note {:role "status" :aria-live "polite"
                             :class (when @state (str "is-" (name @state)))}
          (case @state
            :copied "Copied"
            :failed "Press and hold the number to copy it."
            "In-game number. Tap to copy.")]]))))

(defn rule
  "Thin cream line with a little pine tree in the middle."
  []
  [:div.rule {:aria-hidden "true"}
   [:span.rule__line]
   [i/pines]
   [:span.rule__line]])

(defn section-head
  [{:keys [id title kicker]}]
  [:header.section-head
   (when kicker [:p.kicker kicker])
   [:h2.section-title.distress {:id id} title]
   [rule]])
