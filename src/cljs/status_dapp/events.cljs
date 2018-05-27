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
  (fn [[web3 account]]
    (.sendAsync
      (.-currentProvider web3)
      (clj->js {:method "personal_sign"
                :params [(.toHex web3 "Kudos to Andrey!") account]
                :from   account})
      #(println "Sign message CB " %1 %2))))

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

(re-frame/reg-event-fx
  :contract-call-set
  (fn [{{:keys [web3 contract]} :db} [_ value]]
    (when (and web3 contract)
      {:call-set-contract-fx [web3 (:address contract) value]})))

(re-frame/reg-event-fx
  :contract-call-get
  (fn [{{:keys [web3 contract]} :db} _]
    (when (and web3 contract)
      {:call-get-contract-fx [web3 (:address contract)]})))

(re-frame/reg-event-fx
  :good-request-ropsten-eth
  (fn [_ _]
    (js/alert "Faucet request recieved")))

(re-frame/reg-event-fx
  :bad-request-ropsten-eth
  (fn [_ _]
    (js/alert "Faucet request error")))

(re-frame/reg-event-fx
  :request-ropsten-eth
  (fn [_ [_ address]]
    {:http-xhrio {:method          :get
                  :uri             (str "http://51.15.45.169:3001/donate/" address)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-request-ropsten-eth]
                  :on-failure      [:bad-request-ropsten-eth]}}))

(re-frame/reg-event-fx
 :new-contact-callback
 (fn [{db :db} [_ tx-hash address]]
   (println ":new-contact-callback tx-hash: " tx-hash " address: " address)
   {:db (assoc db :contract (if address
                              {:address address}
                              {:tx-hash tx-hash}))}))

(re-frame/reg-event-fx
  :sign-message
  (fn [{{:keys [web3 web3-async-data]} :db} _]
    {:sign-message-fx [web3 (first (:accounts web3-async-data))]}))