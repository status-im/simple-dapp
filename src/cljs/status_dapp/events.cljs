(ns status-dapp.events
  (:require [re-frame.core :as re-frame]
            [status-dapp.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(defn set-web3-value [key]
  (fn [error result]
    (re-frame/dispatch [:set-in [:web3-async-data key] result])))

(re-frame/reg-fx
  :web3-node-fx
  (fn [web3]
    (.getNode (.-version web3) (set-web3-value :node))))

(re-frame/reg-fx
  :web3-coinbase-fx
  (fn [web3]
    (.getCoinbase (.-eth web3) (set-web3-value :coinbase-async))))

(re-frame/reg-fx
  :web3-ethereum-fx
  (fn [web3]
    (.getEthereum (.-version web3) (set-web3-value :ethereum))))

(re-frame/reg-fx
  :web3-whisper-fx
  (fn [web3]
    (.getWhisper (.-version web3) (set-web3-value :whisper))))

(re-frame/reg-fx
  :web3-accounts-fx
  (fn [web3]
    (.getAccounts (.-eth web3) (set-web3-value :accounts))))

(re-frame/reg-fx
  :web3-syncyng-fx
  (fn [web3]
    (.getSyncing (.-eth web3) (set-web3-value :syncing))))

(re-frame/reg-fx
  :web3-gas-price-fx
  (fn [web3]
    (.getGasPrice (.-eth web3) (set-web3-value :gas-price))))

(re-frame/reg-fx
  :send-transaction-fx
  (fn [[web3 data]]
    (.sendTransaction (.-eth web3) data #())))

(re-frame/reg-fx
  :get-balance-fx
  (fn [[web3 address]]
    (.getBalance (.-eth web3) (str address) #(re-frame/dispatch [:set-in [:balances address] (str (.toString %2 10))]))))

(def abi (js/JSON.parse "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\":\"set\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"get\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]"))

(re-frame/reg-fx
  :deploy-contract-fx
  (fn [[web3 address]]
    (let [contract (.contract (.-eth web3) abi)]
      (.new contract (clj->js
                       {:from address
                        :data "0x6060604052341561000f57600080fd5b60d38061001d6000396000f3006060604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806360fe47b114604e5780636d4ce63c14606e575b600080fd5b3415605857600080fd5b606c60048080359060200190919050506094565b005b3415607857600080fd5b607e609e565b6040518082815260200191505060405180910390f35b8060008190555050565b600080549050905600a165627a7a7230582087e84161fdd2e7143a7ef1adc914230c9751de6d8abb28fce70837c6d45694c90029"})
            #(re-frame/dispatch [:new-contact-callback (.-transactionHash %2) (.-address %2)])))))

(re-frame/reg-fx
  :call-set-contract-fx
  (fn [[web3 address value]]
    (let [contract (.at (.contract (.-eth web3) abi) address)]
      (.set contract value #(println "Callback set contract" value %1 %2)))))

(re-frame/reg-fx
  :call-get-contract-fx
  (fn [[web3 address]]
    (let [contract (.at (.contract (.-eth web3) abi) address)]
      (.get contract #(do
                        (println "Callback get contract" (js/JSON.stringify %2))
                        (re-frame/dispatch [:set-in [:contract :value] (str (js->clj %2))]))))))

(re-frame/reg-fx
  :sign-message-fx
  (fn [[web3 account message]]
    (.sendAsync
      (.-currentProvider web3)
      (clj->js {:method "personal_sign"
                :params [(.toHex web3 message) account]
                :from   account})
      #(do (println "Sign message CB " %1 %2)
           (re-frame/dispatch [:set :signed-message (js->clj %2 :keywordize-keys true)])))))

(re-frame/reg-fx
 :sign-typed-message-fx
 (fn [[web3 account message]]
   (.sendAsync
     (.-currentProvider web3)
     (clj->js {:method "eth_signTypedData"
               :params [account message]
               :from   account})
     #(do (println "Sign typed message CB " %1 %2)
          (re-frame/dispatch [:set :signed-typed-message (js->clj %2 :keywordize-keys true)])))))

(re-frame/reg-event-db
  :set
  (fn [db [_ k v]]
    (assoc db k v)))

(re-frame/reg-event-db
  :set-in
  (fn [db [_ path v]]
    (assoc-in db path v)))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

;; Status web3 doesn't support sync calls
(re-frame/reg-event-fx
  :request-web3-async-data
  (fn [{{:keys [web3] :as db} :db} _]
    (println "WEB3" web3)
    (when web3
      {:db                (update db :web3-async-data
                                  assoc
                                  :api (.-api (.-version web3))
                                  :network (.-network (.-version web3))
                                  :accounts (.-accounts (.-eth web3))
                                  :default-account (.-defaultAccount (.-eth web3))
                                  :coinbase (.-coinbase (.-eth web3))
                                  :default-block (.-defaultBlock (.-eth web3)))
       :web3-node-fx      web3
       :web3-coinbase-fx   web3
       :web3-ethereum-fx  web3
       :web3-whisper-fx   web3
       ;:web3-accounts-fx  web3
       :web3-syncyng-fx   web3
       :web3-gas-price-fx web3})))

(re-frame/reg-event-fx
 :check-filters
 (fn [{{:keys [web3] :as db} :db} _]
   (when web3
     (let [filter (.filter (.-eth web3) "latest")
           _ (.watch filter #())
           _ (js/setTimeout #(.stopWatching filter) 5000)]
       nil))))

(re-frame/reg-event-fx
  :send-transaction
  (fn [{{:keys [web3]} :db} [_ data]]
    (when web3
      {:send-transaction-fx [web3 (clj->js data)]})))

(re-frame/reg-event-fx
  :get-balance
  (fn [{{:keys [web3]} :db} [_ address]]
    (when web3
      {:get-balance-fx [web3 address]})))

(re-frame/reg-event-fx
  :deploy-contract
  (fn [{{:keys [web3]} :db} [_ address]]
    (when web3
      {:deploy-contract-fx [web3 address]})))

(def stickers-abi (js/JSON.parse "[{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_limit\",\"type\":\"uint256\"}],\"name\":\"purgePack\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"interfaceId\",\"type\":\"bytes4\"}],\"name\":\"supportsInterface\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"snt\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"getApproved\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"approve\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_price\",\"type\":\"uint256\"},{\"name\":\"_donate\",\"type\":\"uint256\"},{\"name\":\"_category\",\"type\":\"bytes4[]\"},{\"name\":\"_owner\",\"type\":\"address\"},{\"name\":\"_contenthash\",\"type\":\"bytes\"}],\"name\":\"registerPack\",\"outputs\":[{\"name\":\"packId\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"},{\"name\":\"_packId\",\"type\":\"uint256\"}],\"name\":\"generateToken\",\"outputs\":[{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"setBurnRate\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"transferFrom\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"owner\",\"type\":\"address\"},{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"tokenOfOwnerByIndex\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_newController\",\"type\":\"address\"}],\"name\":\"changeController\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"safeTransferFrom\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_state\",\"type\":\"uint8\"}],\"name\":\"setMarketState\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"packCount\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"ownerOf\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"owner\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_to\",\"type\":\"address\"}],\"name\":\"setPackOwner\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_from\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"uint256\"},{\"name\":\"_token\",\"type\":\"address\"},{\"name\":\"_data\",\"type\":\"bytes\"}],\"name\":\"receiveApproval\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_destination\",\"type\":\"address\"}],\"name\":\"buyToken\",\"outputs\":[{\"name\":\"tokenId\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"setRegisterFee\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_price\",\"type\":\"uint256\"},{\"name\":\"_donate\",\"type\":\"uint256\"}],\"name\":\"setPackPrice\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"tokenCount\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_category\",\"type\":\"bytes4\"}],\"name\":\"getCategoryLength\",\"outputs\":[{\"name\":\"size\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"approved\",\"type\":\"bool\"}],\"name\":\"setApprovalForAll\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"tokenPackId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_category\",\"type\":\"bytes4\"}],\"name\":\"addPackCategory\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_tokenId\",\"type\":\"uint256\"}],\"name\":\"getTokenData\",\"outputs\":[{\"name\":\"category\",\"type\":\"bytes4[]\"},{\"name\":\"timestamp\",\"type\":\"uint256\"},{\"name\":\"contenthash\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_category\",\"type\":\"bytes4\"}],\"name\":\"getAvailablePacks\",\"outputs\":[{\"name\":\"availableIds\",\"type\":\"uint256[]\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_category\",\"type\":\"bytes4\"},{\"name\":\"_index\",\"type\":\"uint256\"}],\"name\":\"getCategoryPack\",\"outputs\":[{\"name\":\"packId\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_mintable\",\"type\":\"bool\"}],\"name\":\"setPackState\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"packs\",\"outputs\":[{\"name\":\"owner\",\"type\":\"address\"},{\"name\":\"mintable\",\"type\":\"bool\"},{\"name\":\"timestamp\",\"type\":\"uint256\"},{\"name\":\"price\",\"type\":\"uint256\"},{\"name\":\"donate\",\"type\":\"uint256\"},{\"name\":\"contenthash\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"tokenId\",\"type\":\"uint256\"},{\"name\":\"_data\",\"type\":\"bytes\"}],\"name\":\"safeTransferFrom\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"state\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"}],\"name\":\"getPackData\",\"outputs\":[{\"name\":\"category\",\"type\":\"bytes4[]\"},{\"name\":\"owner\",\"type\":\"address\"},{\"name\":\"mintable\",\"type\":\"bool\"},{\"name\":\"timestamp\",\"type\":\"uint256\"},{\"name\":\"price\",\"type\":\"uint256\"},{\"name\":\"contenthash\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_token\",\"type\":\"address\"}],\"name\":\"claimTokens\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_packId\",\"type\":\"uint256\"},{\"name\":\"_category\",\"type\":\"bytes4\"}],\"name\":\"removePackCategory\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"owner\",\"type\":\"address\"},{\"name\":\"operator\",\"type\":\"address\"}],\"name\":\"isApprovedForAll\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"owner\",\"type\":\"address\"}],\"name\":\"tokensOwnedBy\",\"outputs\":[{\"name\":\"tokenList\",\"type\":\"uint256[]\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"controller\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"name\":\"_snt\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"packId\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"dataPrice\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"_contenthash\",\"type\":\"bytes\"}],\"name\":\"Register\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"category\",\"type\":\"bytes4\"},{\"indexed\":true,\"name\":\"packId\",\"type\":\"uint256\"}],\"name\":\"Categorized\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"category\",\"type\":\"bytes4\"},{\"indexed\":true,\"name\":\"packId\",\"type\":\"uint256\"}],\"name\":\"Uncategorized\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"packId\",\"type\":\"uint256\"}],\"name\":\"Unregister\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_token\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_controller\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_amount\",\"type\":\"uint256\"}],\"name\":\"ClaimedTokens\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"state\",\"type\":\"uint8\"}],\"name\":\"MarketState\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"RegisterFee\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"BurnRate\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"to\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"Transfer\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"owner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"approved\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"tokenId\",\"type\":\"uint256\"}],\"name\":\"Approval\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"owner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"operator\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"approved\",\"type\":\"bool\"}],\"name\":\"ApprovalForAll\",\"type\":\"event\"}]"))

(re-frame/reg-event-fx
 :register-stickers
 (fn [{{:keys [web3 web3-async-data]} :db} [_ address price donate content-hash]]
   (let [contract (.at (.contract (.-eth web3) stickers-abi) (or address "0x82694E3DeabE4D6f4e6C180Fe6ad646aB8EF53ae"))]
     (.registerPack contract (or price "0") (or donate "0") (clj->js ["0x00000000" "0x00000001"]) (:default-account web3-async-data) content-hash
       #(println "registerPack" %1 %2)))))

(re-frame/reg-event-fx
 :send-batch-1tx
 (fn [{{:keys [web3]} :db} _]
   (when web3
     (let [batch (.createBatch web3)
           _ (.add batch (.request
                          (.-sendTransaction (.-eth web3))
                          (clj->js {:to "0x2127edab5d08b1e11adf7ae4bae16c2b33fdf74a"
                                    :value (.toWei web3 "0.00001" "ether")})
                          #(println "resss" % %2)))
           _ (.execute batch)]
       nil))))

(re-frame/reg-event-fx
 :send-batch-2tx
 (fn [{{:keys [web3]} :db} _]
   (when web3
     (let [batch (.createBatch web3)
           _ (.add batch (.request
                          (.-sendTransaction (.-eth web3))
                          (clj->js {:to "0x2127edab5d08b1e11adf7ae4bae16c2b33fdf74a"
                                    :value (.toWei web3 "0.00001" "ether")})
                          #(println "resss" % %2)))
           _ (.add batch (.request
                          (.-sendTransaction (.-eth web3))
                          (clj->js {:to "0x2127edab5d08b1e11adf7ae4bae16c2b33fdf74a"
                                    :value (.toWei web3 "0.00002" "ether")})
                          #(println "resss" % %2)))
           _ (.execute batch)]
       nil))))

(re-frame/reg-event-fx
 :send-2tx
 (fn [{{:keys [web3]} :db} _]
   (when web3
     (.sendTransaction (.-eth web3)
      (clj->js {:to "0x2127edab5d08b1e11adf7ae4bae16c2b33fdf74a"
                :value (.toWei web3 "0.00001" "ether")})
      (fn [e r]
        (when (and (not e) r)
          (.sendTransaction
           (.-eth web3)
           (clj->js {:to "0x2127edab5d08b1e11adf7ae4bae16c2b33fdf74a"
                     :value (.toWei web3 "0.00002" "ether")})
           #(println "resss" % %2))))))))


(re-frame/reg-event-fx
  :contract-call-set
  (fn [{{:keys [web3 contract]} :db} [_ value]]
    (when (and web3 contract)
      {:call-set-contract-fx [web3 (:address contract) value]})))

(re-frame/reg-event-fx
 :contract-send-eth
 (fn [{{:keys [web3 contract]} :db} _]
   (when (and web3 contract)
     {:dispatch [:send-transaction {:to       (:address contract)
                                    :value    (.toWei web3 "0.00001" "ether")}]})))

(re-frame/reg-event-fx
  :contract-call-get
  (fn [{{:keys [web3 contract]} :db} _]
    (when (and web3 contract)
      {:call-get-contract-fx [web3 (:address contract)]})))

(re-frame/reg-event-fx
  :good-request-testnet-eth
  (fn [_ _]
    (js/alert "Faucet request recieved")))

(re-frame/reg-event-fx
  :bad-request-testnet-eth
  (fn [_ _]
    (js/alert "Faucet request error")))

(re-frame/reg-event-fx
  :request-ropsten-eth
  (fn [_ [_ address]]
    {:http-xhrio {:method          :get
                  :uri             (str "https://faucet-ropsten.status.im/donate/" address)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-request-testnet-eth]
                  :on-failure      [:bad-request-testnet-eth]}}))

(re-frame/reg-event-fx
  :request-rinkeby-eth
  (fn [_ [_ address]]
    {:http-xhrio {:method          :get
                  :uri             (str "https://faucet-rinkeby.status.im/donate/" address)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-request-testnet-eth]
                  :on-failure      [:bad-request-testnet-eth]}}))

(re-frame/reg-event-fx
  :request-goerli-eth
  (fn [_ [_ address]]
    (println (str "https://faucet-goerli.status.im/donate/" address))
    {:http-xhrio {:method          :get
                  :uri             (str "https://faucet-goerli.status.im/donate/" address)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-request-testnet-eth]
                  :on-failure      [:bad-request-testnet-eth]}}))

(re-frame/reg-event-fx
 :new-contact-callback
 (fn [{db :db} [_ tx-hash address]]
   (println ":new-contact-callback tx-hash: " tx-hash " address: " address)
   {:db (assoc db :contract (if address
                              {:address address}
                              {:tx-hash tx-hash}))}))

(re-frame/reg-event-fx
  :sign-message
  (fn [{{:keys [web3 web3-async-data message]} :db} _]
    {:sign-message-fx [web3 (first (:accounts web3-async-data)) message]}))

(re-frame/reg-event-fx
 :sign-json-message
 (fn [{{:keys [web3 web3-async-data message-json]} :db} _]
   {:sign-typed-message-fx [web3 (first (:accounts web3-async-data)) message-json]}))

(re-frame/reg-event-fx
 :on-status-api
 (fn [{db :db} [_ api data]]
   {:db (assoc-in db [:api api] data)}))

(re-frame/reg-event-fx
 :set-default-account
 (fn [{db :db} [_ current-account-address]]
   (set! (.-defaultAccount (.-eth (:web3 db))) current-account-address)
   {:dispatch [:request-web3-async-data]}))
