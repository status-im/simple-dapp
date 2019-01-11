(ns status-dapp.db
  (:require [re-frame.core :as re-frame]))

(def web3 (or (when (exists? js/web3) js/web3)
              #_(when (exists? js/ethereumBeta)
                  (js/setTimeout (fn []
                                   (.then (.send js/ethereumBeta "eth_requestAccounts") #(re-frame/dispatch [:set-default-account]))
                                   (when js/ethereumBeta
                                     (.then (.getContactCode js/ethereumBeta.status) #(re-frame/dispatch [:on-status-api :contact %]))))
                                 100)
                  (js/Web3. js/ethereumBeta))
              (when (exists? js/ethereum)
                (js/setTimeout (fn []
                                 (.then (.enable js/ethereum) #(re-frame/dispatch [:set-default-account]))
                                 (when js/ethereum.status
                                   (.then (.getContactCode js/ethereum.status) #(re-frame/dispatch [:on-status-api :contact %]))))
                               100)
                (js/Web3. js/ethereum))))

(def default-db
  {:web3            web3
   :web3-async-data {}
   :view-id         (if web3 :web3 :no-web3)
   :message         "Test message"
   :tab-view        :accounts})