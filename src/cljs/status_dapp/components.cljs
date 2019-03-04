(ns status-dapp.components
  (:require [status-dapp.react-native-web :as react]
            [re-frame.core :as re-frame]))

(defn button [label on-press]
  [react/touchable-highlight {:on-press on-press}
   [react/view {:style {:flex-direction :row :align-items :center :margin-vertical 5}}
    [react/view {:style {:padding 4 :background-color "#4360df" :border-radius 4}}
     [react/text {:style {:color :white}} label]]]])

(defn label [label value]
  [react/view {:style {:flex-direction :row :align-items :center :margin-vertical 5}}
   [react/view {:style {:padding 4 :background-color "#4360df99" :border-radius 4}}
    [react/text {:style {:color :white}} label]]
   [react/text {:style {:margin-left 10}} value]])

(defn tab-button [label tab-view current-tab-view]
  [react/view {:style {:margin-right 10 :opacity (when (not= tab-view current-tab-view) 0.5)}}
   [button label (when (not= tab-view current-tab-view) #(re-frame/dispatch [:set :tab-view tab-view]))]])

(defn tab-buttons [tab-view]
  [react/view
   [react/view {:style {:flex-direction :row :padding 10 :flex-wrap :wrap}}
    [tab-button "Accounts" :accounts tab-view]
    [tab-button "Assets" :assets tab-view]
    [tab-button "Transactions" :transactions tab-view]
    [tab-button "ETH" :eth tab-view]
    [tab-button "Version" :version tab-view]
    [tab-button "Status API" :api tab-view]
    [tab-button "Stickers" :stickers tab-view]
    (when (exists? js/ethereumBeta)
      [tab-button "Beta Provider" :beta tab-view])
    [tab-button "About" :about tab-view]]
   [react/view {:style {:height 1 :margin-top 10 :background-color "#4360df33"}}]])

(defn asset-button [label asset-address]
  [react/view {:style {:margin-bottom 10}}
   [button (str "Request " label) #(re-frame/dispatch [:send-transaction {:to    asset-address
                                                                          :value 0
                                                                          :gas   150000}])]])