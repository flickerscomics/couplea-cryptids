(ns couplea.data
  "Everything the owners can change comes from site.edn and stock.edn.
   The macros read and check those files while the site is being built,
   and the data is baked into the JavaScript."
  (:require-macros [couplea.macros :as m]))

(def site (m/site-config))
(def stock (m/stock-config))
(def build-date (m/build-date))

(def categories
  [{:id :jerky   :label "Jerky & Snacks"}
   {:id :apples  :label "Caramel Apples"}
   {:id :wood    :label "Wood Working"}
   {:id :candles :label "Candles & Wax"}
   {:id :handmade :label "Handmade Goods"}])

(def category-label
  (into {} (map (juxt :id :label)) categories))

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
