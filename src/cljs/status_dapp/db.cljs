(ns status-dapp.db)

(def default-db
  {:web3            (when (exists? js/web3) js/web3)
   :web3-async-data {}
   :view-id         (if (exists? js/web3) :web3 :no-web3)
   :message         "Test message"
   :tab-view        :accounts})