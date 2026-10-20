(ns couplea.shelves
  "On the Shelves: filter buttons and one card per item."
  (:require [reagent.core :as r]
            [couplea.data :as d]
            [couplea.icons :as i]
            [couplea.ui :as ui]))

(defonce selected-maker (r/atom :all))

(def filters
  [[:all "All"] [:buck "Buck"] [:hunter "Hunter"]])

(defn- shown-items
  "Items for the chosen maker. In stock first, sold out last.
   Within each group the order in stock.edn is kept."
  [maker]
  (->> d/stock
       (filter (fn [item]
                 (or (= maker :all)
                     (= (:maker item) maker)
                     (= (:maker item) :both))))
       (sort-by #(if (:in-stock %) 0 1))))

(defn- empty-message [maker]
  (if (= maker :all)
    "Shelves are bare. Check back soon."
    (str "Nothing from " (d/maker-label maker) " on the shelves right now. Check back soon.")))

(defn item-card
  [{:keys [name description price category maker in-stock tag image icon]}]
  (let [sold? (not in-stock)
        ;; Which picture? 1) the photo, 2) the item's own :icon, 3) the category icon
        icon-name (if (seq icon) icon (d/category-icon category))]
    [:li.item.paper {:class (when sold? "item--sold")}
     [:div.item__inner
      [:div.item__media
       (if (seq image)
         [:img {:src image :alt (str name ", photo") :loading "lazy" :decoding "async"}]
         [:div.item__icon [i/icon-img icon-name {:alt (i/icon-label icon-name)}]])
       (when (seq tag) [:span.tag tag])]
      [:div.item__body
       [:p.item__meta
        (d/category-label category)
        [:span.dot {:aria-hidden "true"} " • "]
        (d/maker-label maker)]
       [:h3.item__name name]
       [:p.item__desc description]
       [:p.item__price {:class (when (= price :ask) "item__price--ask")}
        (d/price-text price)]]]
     (when sold?
       [:div.stamp "SOLD OUT"])]))

(defn shelves []
  (let [maker @selected-maker
        items (shown-items maker)
        in-stock (count (filter :in-stock items))]
    [:section.section {:id "shelves" :aria-labelledby "shelves-title"}
     [:div.wrap
      [:header.section-head
       [:h2.section-title.distress {:id "shelves-title"} "On the Shelves"]
       [ui/rule]
       [:p.updated "Last updated: " d/build-date]]

      [:div.filters {:role "group" :aria-label "Show items made by"}
       (for [[id label] filters]
         ^{:key id}
         [:button.chip {:type "button"
                        :aria-pressed (= maker id)
                        :class (when (= maker id) "is-on")
                        :on-click #(reset! selected-maker id)}
          label])]

      (when (zero? in-stock)
        [:p.bare {:role "status"} (empty-message maker)])

      (when (seq items)
        [:ul.items
         (for [item items]
           ^{:key (:name item)}
           [item-card item])])]]))
