(ns status-dapp.views
  (:require-macros [status-dapp.utils :refer [defview letsubs]])
  (:require [status-dapp.react-native-web :as react]
            [re-frame.core :as re-frame]
            [status-dapp.components :as ui]
            [status-dapp.constants :as constants]))

(defn no-web3 []
  [react/view {:style {:flex 1 :padding 10 :align-items :center :justify-content :center}}
   [react/text {:style {:font-weight :bold}}
    "Can't find web3 library"]])

(defn send-transaction [from]
  (re-frame/dispatch [:send-transaction {:from     from
                                         :to       constants/stt-ropsten-contract
                                         :value    0
                                         :gasPrise 150000}]))

(defview web3-view []
  (letsubs [{:keys [api node network ethereum whisper accounts syncing gas-price
                    default-account default-block]}
            [:get :web3-async-data]]
    [react/scroll-view {:style {:flex 1}}
     [react/view {:style {:flex 1 :padding 10}}
      [react/view {:style {:flex-direction :row}}
       ;[ui/button "Request Ropsten ETH" #(re-frame/dispatch [:request-ropsten-eth (str (first accounts))])]
       ;[react/view {:style {:width 5}}]
       (when (= "3" network)
         [ui/button "Request 1000 STT" #(send-transaction (str (first accounts)))])]
      [react/text {:style {:font-weight :bold :margin-top 20}} "Version"]
      [ui/label "api" api]
      [ui/label "node" node]
      [ui/label "network" (str network " (" (or (constants/chains network) "Unknown") ")")]
      [ui/label "ethereum" ethereum]
      [ui/label "whisper" whisper]
      [react/text {:style {:font-weight :bold :margin-top 20}} "Accounts"]
      [ui/label "defaultAccount" default-account]
      [ui/label "accounts" ""]
      (for [account accounts]
        [react/text account])
      [react/text {:style {:font-weight :bold :margin-top 20}} "Eth"]
      [ui/label "defaultBlock" default-block]
      (if syncing
        [react/view
         [ui/label "isSyncing" "true"]
         [ui/label "startingBlock" (.-startingBlock syncing)]
         [ui/label "currentBlock" (.-currentBlock syncing)]
         [ui/label "highestBlock" (.-highestBlock syncing)]]
        [ui/label "isSyncing" "false"])
      (when gas-price
        [ui/label "gasPrice" (str (.toString gas-price 10) " wei")])]]))

(defview main []
  (letsubs [view-id [:get :view-id]]
    (case view-id
      :web3 [web3-view]
      [no-web3])))