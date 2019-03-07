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
   :message-json    "{\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},\"primaryType\":\"Mail\",\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":3,\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},\"message\":{\"from\":{\"name\":\"Cow\",\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\",\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}"
   :tab-view        :accounts})