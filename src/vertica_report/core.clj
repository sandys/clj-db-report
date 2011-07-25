(ns vertica-report.core
  (:require [clojure.contrib.sql :as SQL]
            [clojure.contrib.json :as JSON]
            [clojure.contrib.trace :as TRACE]
            [ring.util.response :only [response] :as response] 
            [ring.adapter.jetty :only [run-jetty] :as run-jetty]
            [ring.middleware.params :only [wrap-params] :as wrap-params]
            [net.cgrand.enlive-html :as html]
            [net.cgrand.moustache :as moustache]))

(def mysqldb   {:classname "com.mysql.jdbc.Driver" :subprotocol "mysql" :subname "//localhost/drupal_2" :user "drupal" :password "drupal"})
(def verticadb {:classname "com.vertica.Driver" :subprotocol "vertica" :subname "//localhost/db" :user "dbadmin" :password "b1ngo"})

(defn limit-sql)

(defn raw-sql [table] [(str "select * from " table )])


(defn table-query [db query] 
  (SQL/with-connection db 
    (SQL/with-query-results rs query (doall rs))))

;; hmmm.... need the json-str function, else map shuts down the connection after first execution
(defn try [t] 
  (SQL/with-connection mysqldb 
    (JSON/json-str (map #(% :column_name) (resultset-seq 
                            (-> (SQL/connection) (.getMetaData) (.getColumns nil nil t nil) ))))))
(defn get-table-names [t db] 
  (SQL/with-connection db 
    (into #{} (map #(println % ) (resultset-seq 
                            (-> (SQL/connection) (.getMetaData) (.getTables nil nil nil (into-array '("TABLE"))  ) ))))))

(defn get-column-names-vertica [t db] 
;  (into #{} (map #(:column_name % ) (table-query db [(str "select column_name from v_catalog.columns where table_name='" t "'")] ))))
  (into #{} (map #(name %) (keys (first  (vertica-report.core/table-query  vertica-report.core/verticadb   [(str "select * from " t " limit 1")] ))))))

(defn get-column-names [t db] 
  (SQL/with-connection db 
    (into #{} (map #(:column_name % ) (resultset-seq 
                            (-> (SQL/connection) (.getMetaData) (.getColumns nil nil t nil) ))))))

;;;;;;;;;;;;;;;;;


;use by calling (vertica-report.core/table-titles "SSS" )
(html/defsnippet table-titles "vertica_report/template1.html" [[:.titlerow] [:th html/first-child] ]
  [items] 
  [:th] (html/clone-for [item items] (html/content item)))

(html/defsnippet table-data "vertica_report/template1.html"  [[:.datarow] [:td html/first-child] ]
  [title-items data-item-map] 
  [:td] (html/clone-for [title-item title-items] (html/content (str (data-item-map (keyword title-item)) ) ) ) )


(html/deftemplate data-template "vertica_report/template1.html"
  [title-items data-items table-name]
  [:.titlerow] (html/content (table-titles title-items))
  [:.datarow] (html/clone-for [data-item data-items] (html/content (table-data title-items data-item) ) )
  [:.table_name] (html/content table-name)
  )

(defn dispatch [req-params table-name]
  (let [sql (raw-sql table-name) v []] 
    (cond 
      (contains? req-params "limit")   (conj sql " limit " (req-params "limit"))
      :else sql)))

; response/response (str (req "b"))

(def routes 
  (moustache/app  

    ["query" t]            (wrap-params/wrap-params  (fn [req]  (let [req-params (req :params)] 
                                                                  (response/response (apply str ( dispatch  req-params t))))))
    ["table" table-name]  (wrap-params/wrap-params  (fn [req]  (let [req-params (req :params)] 
                                                                   (-> (data-template
                                                                         (get-column-names-vertica table-name verticadb) 
                                                                         (table-query verticadb [(apply str ( dispatch  req-params table-name))]  )
                                                                       table-name) 
                                                                    response/response ))))))

(defonce server (run-jetty/run-jetty #'routes 
                           {:port 8000 :join? false}))

(defn -main [& args] server)
