(ns status-dapp.db
  (:require [re-frame.core :as re-frame]))

(def web3 (or (when (exists? js/web3) js/web3)
              (when js/ethereum
                (js/setTimeout (fn [] (.then (.enable js/ethereum) #(do
                                                                      (re-frame/dispatch [:set-default-account]))) 100))
                (js/Web3. js/ethereum))))

(def default-db
  {:web3            web3
   :web3-async-data {}
   :view-id         (if web3 :web3 :no-web3)
   :message         "Test message"
   :tab-view        :accounts})