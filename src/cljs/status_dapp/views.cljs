(ns status-dapp.views
  (:require-macros [status-dapp.utils :refer [defview letsubs]])
  (:require [status-dapp.react-native-web :as react]
            [re-frame.core :as re-frame]
            [status-dapp.components :as ui]
            [status-dapp.constants :as constants]
            [reagent.core :as reagent]))

(defn no-web3 []
  [react/view {:style {:flex 1 :padding 10 :align-items :center :justify-content :center}}
   [react/text {:style {:font-weight :bold}}
    "Can't find web3 library"]
   [:a {:href "https://join.status.im/b/status-im.github.io/dapp/"} "Open in Status"]])

(defview contract-panel [accounts]
  (letsubs [message [:get :message]
            typed-message [:get :message-json]
            {:keys [result]} [:get :signed-message]
            signed-typed-message [:get :signed-typed-message]
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
     [react/view
      [react/text-input {:style         {:font-size 15 :border-width 1 :border-color "#4360df33" :height 60}
                         :default-value typed-message
                         :multiline     true
                         :on-change     (fn [e]
                                          (let [native-event (.-nativeEvent e)
                                                text         (.-text native-event)]
                                            (re-frame/dispatch [:set :message-json text])))}]]
     [ui/button "Sign Typed Message" #(re-frame/dispatch [:sign-json-message])]
     (when (:result signed-typed-message)
       [react/view {:style {:margin-bottom 10}}
        [ui/label "Signed typed message: " ""]
        [react/text {:style {:flex 1}} (str (:result signed-typed-message))]])
     [react/view {:style {:height 20}}]
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
      [ui/button "Send two Txs in batch, 0.00001 and 0.00002 ETH" #(re-frame/dispatch [:send-batch-2tx])]
      [ui/button "Send two Txs, one after another, 0.00001 and 0.00002 ETH" #(re-frame/dispatch [:send-2tx])]]
     [react/view {:style {:margin-top 30}}
      [ui/button "Test filters" #(re-frame/dispatch [:check-filters])]]]))

(defn stickers-input [value placeholder key]
  [react/text-input {:style         {:font-size 15 :border-width 1 :border-color "#4360df33"}
                     :placeholder   placeholder
                     :default-value value
                     :on-change     (fn [e]
                                      (let [native-event (.-nativeEvent e)
                                            text         (.-text native-event)]
                                        (re-frame/dispatch [:set-in [:stickers key] text])))}])

(defview web3-view []
  (letsubs [{:keys [api node network ethereum whisper accounts syncing gas-price
                    default-account coinbase coinbase-async default-block]}
            [:get :web3-async-data]
            status-api [:get :api]
            web3       [:get :web3]
            tab-view   [:get :tab-view]
            balances   [:get :balances]
            {:keys [content-hash price donate contract]} [:get :stickers]
            qr-result (reagent/atom "")]
    [react/view {:style {:flex 1}}
     [ui/tab-buttons tab-view]
     [react/scroll-view {:style {:flex 1}}
      [react/view {:style {:flex 1 :padding 10}}

       (when (= :assets tab-view)
         [react/view
          (case  network
            "3" [react/view
                 [ui/button "Request Ropsten ETH" #(re-frame/dispatch [:request-ropsten-eth (str (first accounts))])]
                 [react/view {:style {:height 20}}]
                 [ui/asset-button "STT" constants/stt-ropsten-contract]
                 [ui/asset-button "HND" constants/hnd-ropsten-contract]
                 [ui/asset-button "LXS" constants/lxs-ropsten-contract]
                 [ui/asset-button "ADI" constants/adi-ropsten-contract]
                 [ui/asset-button "WGN" constants/wgn-ropsten-contract]
                 [ui/asset-button "MDS" constants/mds-ropsten-contract]]
            "4" [react/view
                 [ui/button "Request Rinkeby ETH" #(re-frame/dispatch [:request-rinkeby-eth (str (first accounts))])]]
            "5" [react/view
                 [ui/button "Request Goerli ETH" #(re-frame/dispatch [:request-goerli-eth (str (first accounts))])]
                 [react/view {:style {:height 20}}]
                 [ui/asset-button "STT" constants/stt-goerli-contract]
                 [ui/asset-button "XEENUS" constants/xeenus-goerli-contract]
                 [ui/asset-button "YEENUS" constants/yeenus-goerli-contract]
                 [ui/asset-button "ZEENUS" constants/zeenus-goerli-contract]]
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

       (when (= :beta tab-view)
         [react/view
          [ui/button "Subscribe logs"
           (fn [] (.send js/window.ethereumBeta "eth_subscribe" (clj->js ["syncing" {}])))]])
       (when (= :api tab-view)
         [react/view
          (when (exists? js/window.ethereum.status)
            [react/view
             [ui/button "Request contact code (public key)"
              (fn [] (.then (.getContactCode js/window.ethereum.status) #(re-frame/dispatch [:on-status-api :contact %])))]
             [react/view {:style {:margin-bottom 10}}
              [ui/label "Contact code: " ""]
              [react/text (:contact status-api)]]])

          [ui/button "Install principles extension"
           (fn []
             (let [uri "https://join.status.im/extension/ipfs@QmcfsYnFvKXApcFTCNttpQvQKYxiMCqVx5MvsocFyrr2KA"]
               (if (and web3 (.-currentProvider web3) (.-installExtension (.-currentProvider web3)))
                 (.installExtension (.-currentProvider web3) uri)
                 (when (exists? js/window.ethereum.status)
                   (.installExtension js/window.ethereum.status uri)))))]

          [ui/button "Scan QR"
           (fn [] (if (and web3 (.-currentProvider web3) (.-scanQRCode (.-currentProvider web3)))
                    (.catch (.then (.scanQRCode (.-currentProvider web3))
                                   #(reset! qr-result %))
                            #(reset! qr-result (str "Error" %)))
                    (reset! qr-result "No QR API found")))]

          [react/view {:style {:margin-bottom 10}}
           [ui/label "Scan QR result: " ""]
           [react/text @qr-result]]])

       (when (= :deep-links tab-view)
         [react/view
          [react/view {:style {:flex-direction :row :padding-vertical 10}}
           [react/link {:href "ethereum:0x89205a3a3b2a69de6dbf7f01ed13b2108b2c43e7"}
             "EIP681 deep-link"]]])

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
          [react/text {:selectable true} "Sources: https://github.com/status-im/status-dapp"]])

       (when (= :stickers tab-view)
         [react/view {:style {:margin-bottom 10}}
          [stickers-input contract "Contract" :contract]
          [stickers-input price "Price" :price]
          [stickers-input donate "Donate" :donate]
          [stickers-input content-hash "Content hash" :content-hash]
          [ui/button "Register" #(re-frame/dispatch [:register-stickers contract price donate content-hash])]])]]]))

(defview dapp-store-view []
  (letsubs [{:keys [dapps]} [:get :dapp-store]]
    [react/scroll-view
     (for [{:keys [title data color]} dapps]
       [react/view {:style {:margin 16}}
        [react/view {:style {:height 40
                             :padding-horizontal 14
                             :justify-content :center
                             :background-color color
                             :box-shadow "0px 2px 8px rgba(0, 0, 0, 0.1), 0px 2px 6px rgba(136, 122, 249, 0.2)"
                             :border-radius 8
                             :margin-bottom 12}}
         [react/text {:style {:font-size 15 :font-weight "500" :color :white}} title]]
        (for [{:keys [name dapp-url description]} data]
          [react/touchable-highlight {:on-press #(.open js/window dapp-url "_self")}
           [react/view {:style {:height 96 :padding-top 11}}
            [react/text {:style {:font-size 15 :line-height 22 :font-weight "500"}} name]
            [react/text {:style {:font-size 13 :color "#939BA1" :line-height 18 :margin-top 5 :margin-bottom 2}} description]
            [react/text {:style {:font-size 12 :color "#4360DF"}} (str dapp-url " ->")]]])])]))

(defview main []
  (letsubs [view-id [:get :view-id]]
    (case view-id
      :web3 [web3-view]
      :dapp-store [dapp-store-view]
      [no-web3])))
