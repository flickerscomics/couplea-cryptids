(ns couplea.data
  "Everything the owners can change comes from site.edn and stock.edn.
   The macros read and check those files while the site is being built,
   and the data is baked into the JavaScript."
  (:require-macros [couplea.macros :as m]))

(def site (m/site-config))
(def stock (m/stock-config))
(def build-date (m/build-date))

;; The five kinds of goods. :icon is the picture used for the category
;; (a file in static/art/icons/, without .svg) and for any item that
;; has no photo and no :icon of its own.
(def categories
  [{:id :jerky    :label "Jerky & Snacks" :icon "beef-jerky"}
   {:id :apples   :label "Caramel Apples" :icon "caramel-apple"}
   {:id :wood     :label "Wood Working"   :icon "pine-tree-wood-sign"}
   {:id :candles  :label "Candles & Wax"  :icon "pine-tree-jar-candle"}
   {:id :handmade :label "Handmade Goods" :icon "basket-plant"}])

(def category-label
  (into {} (map (juxt :id :label)) categories))

(def category-icon
  (into {} (map (juxt :id :icon)) categories))

(def maker-label
  {:buck "Buck" :hunter "Hunter" :both "Buck & Hunter"})

(defn- with-commas [n]
  (.replace (str n) (js/RegExp. "\\B(?=(\\d{3})+(?!\\d))" "g") ","))

(defn price-text
  "300 -> \"$300\", 1250 -> \"$1,250\", :ask -> \"Ask us\"."
  [p]
  (if (= p :ask)
    "Ask us"
    (str "$" (with-commas p))))
