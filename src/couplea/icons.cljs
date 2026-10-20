(ns couplea.icons
  "Pictures for the site.

   The item and category icons are plain SVG files in static/art/icons/.
   They are loaded as images (not pasted into the page), so the page stays light.
   The few small marks at the bottom (phone, pine trees, menu) are tiny inline SVGs."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------- item + category icons

(def icons-folder "art/icons/")

(defn icon-label
  "\"dill-pickles-jar\" -> \"Dill pickles jar\". Used as the picture's alt text."
  [icon-name]
  (let [words (str/replace icon-name "-" " ")]
    (str (str/upper-case (subs words 0 1)) (subs words 1))))

(defn icon-img
  "An icon picture. `icon-name` has no .svg, like \"beef-jerky\".
   Options:
     :alt    text for screen readers. Leave out (or \"\") when the words
             next to the picture already say the same thing.
   Candle icons get a gentle flicker (switched off for reduce-motion)."
  ([icon-name] (icon-img icon-name {}))
  ([icon-name {:keys [alt]}]
   [:img.icon-img {:class (when (str/includes? icon-name "candle") "icon-img--candle")
                   :src (str icons-folder icon-name ".svg")
                   :alt (or alt "")
                   :loading "lazy"
                   :decoding "async"}]))

;; ---------------------------------------------------------------- small marks

(defn phone-badge
  "Round handset badge, like the one on the business card."
  []
  [:svg {:class "i-phone" :viewBox "0 0 48 48" :aria-hidden "true" :focusable "false"}
   [:circle {:cx 24 :cy 24 :r 22 :fill "currentColor"}]
   [:path {:class "cut"
           :d "M16.4 14.6c-1.300.800-2.100 2.200-1.700 3.900 1.600 6.800 6.300 12 12.900 14.700 1.800.700 3.200.100 4.100-1 .8-1 .6-2.200-.2-2.900l-2.400-2c-.7-.6-1.700-.6-2.400 0l-1 .9c-2.900-1.200-5-3.300-6.200-6.200l1-.9c.7-.6.800-1.700.2-2.400l-1.900-2.500c-.7-.9-1.600-1.100-2.400-.6z"}]])

(defn pines
  "Three little pine-tree marks, like the bottom of the flyer."
  []
  [:svg {:class "i-pines" :viewBox "0 0 60 30" :aria-hidden "true" :focusable "false"}
   [:path {:fill "currentColor"
           :d "M30 1L24 11H27L21 20H26V29H34V20H39L33 11H36ZM11 8L7 15H9L5 22H9V29H13V22H17L13 15H15ZM49 8L45 15H47L43 22H47V29H51V22H55L51 15H53Z"}]])

(defn menu []
  [:svg {:class "i-menu" :viewBox "0 0 24 24" :aria-hidden "true" :focusable "false"}
   [:path {:d "M3 6H21M3 12H21M3 18H21" :fill "none" :stroke "currentColor"
           :stroke-width 2.600 :stroke-linecap "round"}]])

(defn close []
  [:svg {:class "i-menu" :viewBox "0 0 24 24" :aria-hidden "true" :focusable "false"}
   [:path {:d "M5 5L19 19M19 5L5 19" :fill "none" :stroke "currentColor"
           :stroke-width 2.600 :stroke-linecap "round"}]])
