(ns status-dapp.views
  (:require-macros [status-dapp.utils :refer [defview letsubs]])
  (:require [status-dapp.react-native-web :as react]
            [re-frame.core :as re-frame]
            [status-dapp.components :as ui]
            [status-dapp.constants :as constants]))

(defn no-web3 []
  [react/view {:style {:flex 1 :padding 10 :align-items :center :justify-content :center}}
   [react/text {:style {:font-weight :bold}}
    "Can't find web3 library"]
   [:a {:href "https://get.status.im/browse/status-im.github.io/dapp/"} "Open in Status"]])

(defview contract-panel [accounts]
  (letsubs [message [:get :message]
            {:keys [result]} [:get :signed-message]
            {:keys [tx-hash address value]} [:get :contract]]
    [react/view
     [react/view {:style {:margin-bottom 10 :flex-direction :row :align-items :center}}
      [react/text-input {:style         {:font-size 15 :border-width 1 :border-color "#4360df33"}
                         :default-value message
                         :on-change     (fn [e]
                                          (let [native-event (.-nativeEvent e)
                                                text         (.-text native-event)]
                                            (re-frame/dispatch [:set :message text])))}]
      [ui/button "Sign message" #(re-frame/dispatch [:sign-message])]]
     (when result
       [react/view {:style {:margin-bottom 10}}
        [ui/label "Signed message: " ""]
        [react/text {:style {:flex 1}} (str result)]])
     (cond

       address
       [react/view
        [ui/label "Contract deployed at: " ""]
        [react/text address]

        [ui/button "Call contract get function" #(re-frame/dispatch [:contract-call-get])]
        [react/text "Default value: 0"]
        (when value
          [react/text value])

        [ui/button "Call contract set function" #(re-frame/dispatch [:contract-call-set 1])]
        [react/text "Sets value to 1"]

        [ui/button "Call function 2 times in a row" #(do
                                                       (re-frame/dispatch [:contract-call-set 10])
                                                       (re-frame/dispatch [:contract-call-set 20]))]
        [react/text "First tx sets value to 10, second to 20"]

        [ui/button "Send 0.00001 ETH to contract" #(re-frame/dispatch [:contract-send-eth])]]

       tx-hash
       [react/view {:style {:padding-top 10}}
        [react/activity-indicator {:animating true}]
        [react/text {:selectable true} (str "Mining new contract in tx: " tx-hash)]]

       :else
       [ui/button "Deploy simple contract" #(re-frame/dispatch [:deploy-contract (str (first accounts))])])
     [react/view {:style {:margin-top 30}}
      [ui/button "Send one Tx in batch" #(re-frame/dispatch [:send-batch-1tx])]
      [ui/button "Send two Txs in batch, 0.00001 and 0.00002 ETH" #(re-frame/dispatch [:send-batch-2tx])]]
     [react/view {:style {:margin-top 30}}
      [ui/button "Test filters" #(re-frame/dispatch [:check-filters])]]]))

(defview web3-view []
  (letsubs [{:keys [api node network ethereum whisper accounts syncing gas-price
                    default-account coinbase coinbase-async default-block]}
            [:get :web3-async-data]
            status-api [:get :api]
            web3       [:get :web3]
            tab-view   [:get :tab-view]
            balances   [:get :balances]]
    [react/view {:style {:flex 1}}
     [ui/tab-buttons tab-view]
     [react/scroll-view {:style {:flex 1}}
      [react/view {:style {:flex 1 :padding 10}}

       (when (= :assets tab-view)
         [react/view
          (case  network
            "3" [react/view
                 [ui/button "Request Ropsten ETH" #(re-frame/dispatch [:request-ropsten-eth (str (first accounts))])]
                 [ui/asset-button "STT" constants/stt-ropsten-contract]
                 [ui/asset-button "HND" constants/hnd-ropsten-contract]
                 [ui/asset-button "LXS" constants/lxs-ropsten-contract]
                 [ui/asset-button "ADI" constants/adi-ropsten-contract]
                 [ui/asset-button "WGN" constants/wgn-ropsten-contract]
                 [ui/asset-button "MDS" constants/mds-ropsten-contract]]
            "4" [react/view
                 [ui/button "Request Rinkeby ETH" #(re-frame/dispatch [:request-rinkeby-eth (str (first accounts))])]]
            [react/text "Assets supported only in Ropsten Testnet"])])

       (when (= :transactions tab-view)
         [contract-panel accounts])

       (when (= :version tab-view)
         [react/view
          [react/text {:style {:font-weight :bold :margin-top 20}} "Version"]
          [ui/label "api" api]
          [ui/label "node" node]
          [ui/label "network" (str network " (" (or (constants/chains network) "Unknown") ")")]
          [ui/label "ethereum" ethereum]
          [ui/label "whisper" whisper]])

       (when (= :accounts tab-view)
         [react/view
          [react/text {:style {:font-weight :bold :margin-top 20}} "Accounts"]
          [ui/label "defaultAccount" ""]
          [react/text default-account]
          [ui/label "coinbase" ""]
          [react/text coinbase]
          [ui/label "coinbase async" ""]
          [react/text coinbase-async]
          [ui/label "accounts" ""]
          (for [account accounts]
            ^{:key account}
            [react/view
             [react/text account]
             [ui/button "Get balance" #(re-frame/dispatch [:get-balance account])]
             (when (get balances account)
               [react/text (str "Balance: " (get balances account) " wei")])])])

       (when (= :eth tab-view)
         [react/view
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
            [ui/label "gasPrice" (str (.toString gas-price 10) " wei")])])

       (when (= :api tab-view)
         [react/view

          [ui/button "Request contact code (public key)"
           #(js/window.postMessage
             (clj->js {:type "STATUS_API_REQUEST" :permissions ["CONTACT_CODE" "CONTACTS"]})
             "*")]
          [react/view {:style {:margin-bottom 10}}
           [ui/label "Contact code: " ""]
           [react/text (:contact status-api)]]])

       (when (= :about tab-view)
         [react/view
          [react/view {:style {:flex-direction :row :padding-vertical 10}}
           [react/text "web3 provider: "]
           (cond (.-currentProvider.isStatus web3)
                 [react/text "Status"]
                 (.-currentProvider.isMetaMask web3)
                 [react/text "MetaMask"]
                 :else [react/text "Unknown"])]

          [react/text "Simple DApp"]
          [react/text {:selectable true} "Sources: https://github.com/status-im/status-dapp"]])]]]))

(defview main []
  (letsubs [view-id [:get :view-id]]
    (case view-id
      :web3 [web3-view]
      [no-web3])))