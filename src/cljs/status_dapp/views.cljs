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

(defview contract-panel [accounts]
  (letsubs [{:keys [tx-hash mining? address value]} [:get :contract]]
    (cond

      address
      [react/view
       [ui/label "Contract deployed at: " ""]
       [react/text address]
       [ui/button "Call contract get function" #(re-frame/dispatch [:contract-call-get])]
       (when value
         [react/text value])
       [ui/button "Call contract set function" #(re-frame/dispatch [:contract-call-set])]]

      tx-hash
      [react/view {:style {:padding-top 10}}
       [react/activity-indicator {:animating true}]
       [react/text {:selectable true} (str "Mining new contract in tx: " tx-hash)]]

      :else
      [ui/button "Deploy simple contract" #(re-frame/dispatch [:deploy-contract (str (first accounts))])])))


(defview web3-view []
  (letsubs [{:keys [api node network ethereum whisper accounts syncing gas-price
                    default-account default-block]}
            [:get :web3-async-data]
            balances [:get :balances]]
    [react/scroll-view {:style {:flex 1}}
     [react/view {:style {:flex 1 :padding 10}}
      [react/view {:style {:flex-direction :row}}
       ;[ui/button "Request Ropsten ETH" #(re-frame/dispatch [:request-ropsten-eth (str (first accounts))])]
       ;[react/view {:style {:width 5}}]
       (when (= "3" network)
         [ui/button "Request 1000 STT" #(send-transaction (str (first accounts)))])]
      [contract-panel accounts]
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
        ^{:key account}
        [react/view
         [react/text account]
         (if (get balances account)
           [react/text (str "Balance: " (get balances account) " wei")]
           [ui/button "Get balance" #(re-frame/dispatch [:get-balance account])])])
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