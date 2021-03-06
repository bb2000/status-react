(ns status-im.ui.screens.chat.stickers.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :as re-frame]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.colors :as colors]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.chat.stickers.styles :as styles]))

(defn button [show-stickers?]
  [react/touchable-highlight
   {:on-press (fn [_]
                (re-frame/dispatch [:chat.ui/set-chat-ui-props {:show-stickers? (not show-stickers?)}])
                (react/dismiss-keyboard!))}
   [vector-icons/icon :icons/stickers {:container-style {:margin 14 :margin-right 6}
                                       :color           (if show-stickers? colors/blue colors/gray)}]])

(defn- no-stickers-yet-panel []
  [react/view {:style {:flex 1 :align-items :center :justify-content :center}}
   [vector-icons/icon :icons/stickers-big {:color colors/gray}]
   [react/text {:style {:margin-top 8 :font-size 17}} (i18n/label :t/you-dont-have-stickers)]
   [react/touchable-highlight {:on-press #(do
                                            (re-frame/dispatch [:stickers/load-packs])
                                            (re-frame/dispatch [:navigate-to :stickers]))}
    [react/text {:style {:margin-top 17 :font-size 15 :color colors/blue}}
     (i18n/label :t/get-stickers)]]])

(defn- on-sticker-click [sticker]
  (re-frame/dispatch [:chat.ui/set-chat-ui-props {:show-stickers? false}])
  (re-frame/dispatch [:chat/send-sticker sticker])
  (react/dismiss-keyboard!))

(defn- stickers-panel [stickers]
  [react/scroll-view {:style {:flex 1} :condtent-container-style {:flex 1}}
   [react/view {:style styles/stickers-panel}
    (for [{:keys [uri] :as sticker} stickers]
      ^{:key uri}
      [react/touchable-highlight {:style    {:height 75 :width 75 :margin 5}
                                  :on-press #(on-sticker-click sticker)}
       [react/image {:style {:resize-mode :cover :width "100%" :height "100%"} :source {:uri uri}}]])]])

(defview recent-stickers-panel []
  (letsubs [stickers [:stickers/recent]]
    (if (seq stickers)
      [stickers-panel (map #(hash-map :uri %) stickers)]
      [react/view {:style {:flex 1 :align-items :center :justify-content :center}}
       [vector-icons/icon :icons/stickers-big {:color colors/gray}]
       [react/text {:style {:margin-top 8 :font-size 17}} (i18n/label :t/recently-used-stickers)]])))

(def icon-size 28)

(defn pack-icon [{:keys [id on-press selected? background-color]
                  :or   {background-color colors/gray
                         on-press         #(re-frame/dispatch [:stickers/select-pack id])}} icon]
  [react/touchable-highlight {:on-press on-press}
   [react/view {:style {:align-items :center}}
    [react/view {:style (styles/pack-icon background-color icon-size)}
     icon]
    [react/view {:style {:margin-bottom 5 :height 2 :width 16
                         :background-color (if selected? colors/blue colors/white)}}]]])

(defn pack-for [packs id]
  (some #(when (= id (:id %)) %) packs))

(defview stickers-view []
  (letsubs [selected-pack   [:stickers/selected-pack]
            installed-packs [:stickers/installed-packs-vals]]
    [react/view {:style {:background-color :white :height "40%"}}
     (cond
       (= selected-pack :recent) [recent-stickers-panel]
       (not (seq installed-packs)) [no-stickers-yet-panel]
       (nil? selected-pack) [recent-stickers-panel]
       :else [stickers-panel (:stickers (pack-for installed-packs selected-pack))])
     [react/view {:style {:flex-direction :row :padding-horizontal 4}}
      [pack-icon {:on-press #(do
                               (re-frame/dispatch [:stickers/load-packs])
                               (re-frame/dispatch [:navigate-to :stickers]))
                  :selected? false :background-color colors/blue}
       [vector-icons/icon :icons/add {:width 20 :height 20 :color colors/white}]]
      [react/view {:width 4}]
      [pack-icon {:id :recent :selected? (or (= :recent selected-pack) (and (nil? selected-pack) (seq installed-packs)))}
       [vector-icons/icon :icons/clock]]
      ;; TODO make scrollable
      (for [{:keys [id thumbnail]} installed-packs]
        ^{:key id}
        [pack-icon {:id id :selected? (= id selected-pack)}
         [react/image {:style {:width icon-size :height icon-size :border-radius (/ icon-size 2)}
                       :source {:uri thumbnail}}]])]]))
