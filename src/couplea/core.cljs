(ns couplea.core
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [couplea.data :as d]
            [couplea.icons :as i]
            [couplea.order :as order]
            [couplea.shelves :as shelves]
            [couplea.ui :as ui]))

(def logo-label "Couple'a Cryptids logo: a stag head with big antlers")

(def nav-links
  [["what-we-make" "What We Make"]
   ["shelves" "Shelves"]
   ["makers" "Makers"]
   ["custom-orders" "Custom Orders"]
   ["pinned-up" "Pinned Up"]
   ["find-us" "Find Us"]])

;; ---------------------------------------------------------------- nav

(defn nav []
  (let [open? (r/atom false)
        close! #(reset! open? false)]
    (fn []
      [:header.nav {:on-key-down #(when (= (.-key %) "Escape") (close!))}
       [:div.wrap.nav__bar
        [:a.nav__brand {:href "#top" :on-click close!}
         [:span.logo.logo--nav {:role "img" :aria-label logo-label}]
         [:span.nav__name (:business-name d/site)]]
        [:button.nav__toggle {:type "button"
                              :aria-expanded (boolean @open?)
                              :aria-controls "site-menu"
                              :on-click #(swap! open? not)}
         (if @open? [i/close] [i/menu])
         [:span "Menu"]]
        [:nav#site-menu.nav__links {:class (when @open? "is-open")
                                    :aria-label "Main"}
         (for [[id label] nav-links]
           ^{:key id}
           [:a {:href (str "#" id) :on-click close!} label])]]])))

;; ---------------------------------------------------------------- hero

(defn hero []
  (let [[first-word rest-words] (str/split (:business-name d/site) #" " 2)]
    [:section#top.hero.paper.paper--folded {:aria-labelledby "site-name"}
     [:div.hero__logo
      [:div.logo.logo--hero {:role "img" :aria-label logo-label}]]
     [:div.hero__text
      [:h1#site-name.hero__name.distress
       [:span first-word] " " [:span rest-words]]
      [:div.hero__line {:aria-hidden "true"}]
      [:p.hero__tagline (:tagline d/site)]
      [:div.hero__line.hero__line--thin {:aria-hidden "true"}]
      [ui/phone-button {:class "phone--hero"}]]
     [:div.hero__band
      [:p.hero__short (:tagline-short d/site)]
      [:p.hero__motto (:motto d/site)]]]))

;; ---------------------------------------------------------------- what we make

(defn what-we-make []
  [:section.section {:id "what-we-make" :aria-labelledby "make-title"}
   [:div.wrap
    [:header.section-head
     [:h2.section-title.distress {:id "make-title"} "What We Make"]
     [ui/rule]]
    [:ul.makes
     (for [{:keys [id label]} d/categories]
       ^{:key id}
       [:li.make
        [:div.make__icon [i/category id]]
        [:p.make__label label]])]]])

;; ---------------------------------------------------------------- makers

(defn maker-card [k icon-k]
  (let [{:keys [name role bio makes]} (get-in d/site [:makers k])]
    [:article.maker.paper.paper--folded
     [:div.maker__icon {:aria-hidden "true"} [i/category icon-k]]
     [:h3.maker__name.distress name]
     [:p.maker__role role]
     [:p.maker__bio bio]
     [:p.maker__makes-title "Makes"]
     [:ul.maker__makes
      (for [m makes]
        ^{:key m} [:li m])]]))

(defn makers []
  [:section.section {:id "makers" :aria-labelledby "makers-title"}
   [:div.wrap
    [:header.section-head
     [:h2.section-title.distress {:id "makers-title"} "Meet the Makers"]
     [ui/rule]
     [:p.lede "Two country boys, one workshop. Neither of us is the cryptid. Probably."]]
    [:div.makers
     [maker-card :buck :wood]
     [maker-card :hunter :jerky]]]])

;; ---------------------------------------------------------------- pinned up

(def pins
  [{:id "card"  :label "Business card" :alt "Our business card: a stag head, the name Couple'a Cryptids and the number 285538"
    :thumb "art/card-thumb.webp" :full "art/card.webp" :w 640 :h 366 :tilt "-2.2deg" :class "pin--card"}
   {:id "flyer" :label "Flyer" :alt "Our flyer: the stag logo, our five kinds of goods and the number 285538"
    :thumb "art/flyer-thumb.webp" :full "art/flyer.webp" :w 560 :h 725 :tilt "1.8deg" :class "pin--flyer"}
   {:id "badge" :label "Badge" :alt "Our round badge: a stag head in the pines, with Buck and Hunter and the number 285538"
    :thumb "art/badge-thumb.webp" :full "art/badge.webp" :w 480 :h 480 :tilt "-1.4deg" :class "pin--badge"}])

(defn pinned-up []
  (let [dlg (atom nil)
        shown (r/atom nil)]
    (fn []
      [:section.section {:id "pinned-up" :aria-labelledby "pinned-title"}
       [:div.wrap
        [:header.section-head
         [:h2.section-title.distress {:id "pinned-title"} "Pinned Up"]
         [ui/rule]
         [:p.lede "Spotted around town. Like a cryptid, but easier to find. Tap one to see it big."]]
        [:div.board
         (for [{:keys [id label alt thumb w h tilt class] :as pin} pins]
           ^{:key id}
           [:figure.pin {:class class :style {"--tilt" tilt}}
            [:span.tack {:aria-hidden "true"}]
            [:button.pin__btn {:type "button"
                               :aria-label (str "View the " (str/lower-case label) " full size")
                               :on-click (fn [_]
                                           (reset! shown pin)
                                           (when-let [el @dlg]
                                             (when-not (.-open el) (.showModal el))))}
             [:img {:src thumb :alt alt :width w :height h :loading "lazy" :decoding "async"}]]
            [:figcaption label]])]
        [:dialog.lightbox {:ref #(reset! dlg %)
                           :aria-label (str "Full size " (some-> @shown :label str/lower-case))
                           :on-close #(reset! shown nil)
                           :on-click (fn [e]
                                       ;; a click on the dark backdrop closes it
                                       (when (= (.-target e) @dlg) (.close @dlg)))}
         (when-let [{:keys [full alt]} @shown]
           [:div.lightbox__inner
            [:img {:src full :alt alt}]
            [:button.lightbox__close {:type "button" :aria-label "Close"
                                      :on-click #(.close @dlg)}
             [i/close]]])]]])))

;; ---------------------------------------------------------------- find us

(defn find-us []
  [:section.section {:id "find-us" :aria-labelledby "find-title"}
   [:div.wrap.wrap--narrow
    [:header.section-head
     [:h2.section-title.distress {:id "find-title"} "Find Us"]
     [ui/rule]]
    [:div.find.paper.paper--folded
     [:div.find__row
      [:p.find__label "Call"]
      [ui/phone-button {:class "phone--find"}]]
     [:div.find__row
      [:p.find__label "Where"]
      [:p.find__text (:location d/site)]]
     [:div.find__row
      [:p.find__label "Hours"]
      [:ul.find__hours
       (for [line (:hours d/site)]
         ^{:key line} [:li line])]]]]])

;; ---------------------------------------------------------------- page

(defn footer []
  [:footer.footer
   [:div.wrap
    [i/pines]
    [:p "(( Couple'a Cryptids is a roleplay business on GTA World. ))"]]])

(defn app []
  [:<>
   [i/rough-filter]
   [:a.skip {:href "#shelves"} "Skip to the shelves"]
   [nav]
   [:main
    [:div.wrap.wrap--hero [hero]]
    [what-we-make]
    [shelves/shelves]
    [makers]
    [order/custom-orders]
    [pinned-up]
    [find-us]]
   [footer]])

(defonce root (atom nil))

(defn ^:dev/after-load render! []
  (when-let [el (.getElementById js/document "app")]
    (when-not @root
      (reset! root (rdom/create-root el)))
    (.render @root (r/as-element [app]))))

(defn init []
  (render!))
