(ns status-dapp.events
  (:require [re-frame.core :as re-frame]
            [status-dapp.db :as db]
            [day8.re-frame.http-fx]))

(defn set-web3-value [key]
  (fn [error result]
    (re-frame/dispatch [:set-in [:web3-async-data key] result])))

(re-frame/reg-fx
  :web3-node-fx
  (fn [web3]
    (.getNode (.-version web3) (set-web3-value :node))))

(re-frame/reg-fx
  :web3-network-fx
  (fn [web3]
    (.getNetwork (.-version web3) (set-web3-value :network))))

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
                                  :default-account (.-defaultAccount (.-eth web3))
                                  :default-block (.-defaultBlock (.-eth web3)))
       :web3-node-fx      web3
       :web3-network-fx   web3
       :web3-ethereum-fx  web3
       :web3-whisper-fx   web3
       :web3-accounts-fx  web3
       :web3-syncyng-fx   web3
       :web3-gas-price-fx web3})))

(re-frame/reg-event-fx
  :send-transaction
  (fn [{{:keys [web3]} :db} [_ data]]
    (when web3
      {:send-transaction-fx [web3 (clj->js data)]})))

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
    (js/alert "Requested")
    {:http-xhrio {:method          :get
                  :uri             (str "http://51.15.45.169:3001/donate/" address)
                  :on-success      [:good-request-ropsten-eth]
                  :on-failure      [:bad-request-ropsten-eth]}}))