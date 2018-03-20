(ns status-dapp.components
  (:require [status-dapp.react-native-web :as react]))

(defn button [label on-press]
  [react/touchable-highlight {:on-press on-press}
   [react/view {:style {:flex-direction :row :align-items :center :margin-top 10}}
    [react/view {:style {:padding 4 :background-color "#4360df" :border-radius 4}}
     [react/text {:style {:color :white}} label]]]])

(defn label [label value]
  [react/view {:style {:flex-direction :row :align-items :center :margin-top 10}}
   [react/view {:style {:padding 4 :background-color "#4360df99" :border-radius 4}}
    [react/text {:style {:color :white}} label]]
   [react/text {:style {:margin-left 10}} value]])