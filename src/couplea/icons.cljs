(ns couplea.icons
  "Original line-art icons, drawn to match the flyer. Everything is plain SVG
   using currentColor, so CSS sets the colour. The wobbly hand-drawn edge comes
   from the #rough filter in rough-filter (rendered once at the top of the page).")

(defn rough-filter
  "Invisible SVG that holds the shared 'hand-drawn wobble' filter."
  []
  [:svg {:class "svg-defs" :width 0 :height 0 :aria-hidden "true" :focusable "false"}
   [:filter {:id "rough" :x "-5%" :y "-5%" :width "110%" :height "110%"}
    [:feTurbulence {:type "fractalNoise" :baseFrequency "0.05" :numOctaves 2 :seed 7 :result "n"}]
    [:feDisplacementMap {:in "SourceGraphic" :in2 "n" :scale 2.6
                         :xChannelSelector "R" :yChannelSelector "G"}]]])

(defn- svg [vb & children]
  (into [:svg {:class "icon" :viewBox vb :aria-hidden "true" :focusable "false"}]
        children))

(defn- leaf [x y angle]
  [:path {:transform (str "translate(" x " " y ") rotate(" angle ")")
          :d "M0 0C-5-4-5-11 0-15C5-11 5-4 0 0Z"}])

;; ---------------------------------------------------------------- categories

(defn- jerky-strip [dx dy]
  [:g {:transform (str "translate(" dx " " dy ")")}
   [:path {:d "M6 53.500L32 19.500Q38 17 41.500 21L44 28.500L18 62.500L15.500 59L13 60.500L11 56.500L9 58Z"}]
   [:path {:d "M15 57L36 29M22 61L29 52"}]])

(defn jerky []
  (svg "0 0 100 100"
       [:g {:transform "translate(10 -2)"}
        [jerky-strip 0 0]
        [jerky-strip 15 11.500]
        [jerky-strip 30 23]]))

(defn apples []
  (svg "0 0 100 100"
       [:path {:d "M50 33C40 24 20 30 20 54C20 74 34 90 50 84C66 90 80 74 80 54C80 30 60 24 50 33Z"}]
       [:path {:class "thick" :d "M52 30L58 7"}]
       ;; caramel coat with drips
       [:path {:class "solid"
               :d "M21 50C22 41 32 36 50 36C68 36 78 41 79 50L79 57C77 61 75 61 73 58L72 70C72 77 65 77 65 70L65 61C63 65 59 65 57 59L56 78C56 85 48 85 48 78L47 61C44 65 40 65 38 61L36 67C35 72 29 72 29 67L28 61C25 62 22 60 21 57Z"}]
       [:path {:class "cut" :d "M30 46C34 42 38 41 43 40"}]
       [:path {:d "M32 91C42 96 58 96 68 91"}]))

(defn wood []
  (svg "0 0 100 100"
       [:g {:transform "rotate(8 50 50)"}
        [:path {:d "M28 9L40 6L52 9L64 6L74 9L75 91L64 94L52 91L40 94L27 91Z"}]
        [:path {:d "M33 18L33 50M33 62L33 84M69 16L69 40M69 54L69 82"}]
        [:path {:class "solid"
                :d "M52 22L45 38L48 38L41 54L45 54L38 70L66 70L59 54L63 54L56 38L59 38Z"}]
        [:path {:class "solid" :d "M50 70H54V80H50Z"}]]))

(defn candles []
  (svg "0 0 100 100"
       [:path {:class "solid flame" :d "M50 5C43 15 41 24 50 31C59 24 57 15 50 5Z"}]
       [:path {:d "M50 31L50 37"}]
       [:rect {:x 26 :y 37 :width 48 :height 10 :rx 3}]
       [:path {:d "M28 47C26 47 24 49 24 52V84C24 88 27 91 31 91H69C73 91 76 88 76 84V52C76 49 74 47 72 47"}]
       [:path {:class "solid" :d "M50 55L42 69L46 69L39 81L61 81L54 69L58 69Z"}]
       [:path {:class "solid" :d "M48 81H52V87H48Z"}]
       [:path {:d "M31 57V80" :opacity ".6"}]))

(defn handmade []
  (svg "0 0 100 100"
       [:path {:d "M16 54H84L76 90H24Z"}]
       [:path {:class "thick" :d "M13 54H87"}]
       [:path {:d "M19 66H81M22 78H78M33 54V66M45 54V66M57 54V66M69 54V66M27 66V78M39 66V78M51 66V78M63 66V78M75 66V78"}]
       [:path {:d "M40 54C38 42 34 32 30 24M50 54C50 42 50 30 50 18M60 54C62 42 66 32 70 24"}]
       [:g
        [leaf 36 40 -38] [leaf 34 31 28] [leaf 31 24 -30]
        [leaf 50 42 -42] [leaf 50 33 40] [leaf 50 24 -28] [leaf 50 19 6]
        [leaf 64 40 38] [leaf 66 31 -28] [leaf 69 24 30]]))

(def category-icons
  {:jerky jerky :apples apples :wood wood :candles candles :handmade handmade})

(defn category
  "The icon for a category keyword."
  [k]
  (when-let [f (category-icons k)]
    [f]))

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
