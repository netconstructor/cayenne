(ns cayenne.core
  (:use clojure.java.io)
  (:use cayenne.conf)
  (:use cayenne.sources.wok)
  (:use cayenne.tasks.dump)
  (:use cayenne.tasks.citation)
  ;;(:use cayenne.tasks.neo4j)
  (:require [cayenne.oai :as oai])
  (:require [cayenne.html :as html])
  (:use clojure.tools.trace)
  (:use [cayenne.formats.unixref :only [unixref-record-parser unixref-citation-parser]]))

(defn scrape-journal-short-names-from-wok []
  (html/scrape-urls journal-pages :scraper journal-names-scraper :task (record-writer "out.txt")))

(def j (file (str (get-param [:dir :test-data]) "/j.xml")))
(def b (file (str (get-param [:dir :test-data]) "/b.xml")))
(def s (file (str (get-param [:dir :test-data]) "/s.xml")))
(def funder-crossmark (file (str (get-param [:dir :test-data]) "/funder-crossmark.xml")))
(def funder-no-crossmark (file (str (get-param [:dir :test-data]) "/funder-no-crossmark.xml")))

(defn parse-oai [file-or-dir]
  (oai/process
   file-or-dir
   :name :parse
   :parser unixref-record-parser 
   :task (record-json-writer "out.txt")))

; todo load-oai should insert objects with ids into mongo and create
; rels between objects with ids in neo4j and update solr

; may also want to attach bits and pieces, such as science categories
; maybe keep science cats in memory?

;; (defn load-oai [file-or-dir]
;;   (oai/process 
;;    file-or-dir
;;    :name :load
;;    :parser unixref-record-parser 
;;    :task (record-neo-inserter)))

(defn check-url-citations [file-or-dir]
  (oai/process
   file-or-dir
   :name :check-url-citations
   :parser unixref-record-parser
   :task (url-citation-checker "check.log.txt")))

(defn find-citations-like [file-or-dir patt]
  (oai/process
   file-or-dir
   :name :find-citations
   :parser unixref-citation-parser 
   :task (matching-citation-finder "match.log.txt" patt)))

(defn find-standards-citations [file-or-dir]
  (let [patt #"^(ASTM [A-G]|ISO |IEC |ISO/IEC |EN |EN ISO |BS |BS ISO |BS EN ISO |IEEE [A-Z]?)[0-9]+((\.|-)[0-9]+)? ((\.|-)[0-9]+)?(:[0-9]{4})?"]
    (find-citations-like file-or-dir patt)))

(defn find-standards-citations-loose [file-or-dir]
  (let [patt #"(ASTM [A-G]|ISO |IEC |ISO/IEC |EN |EN ISO |BS |BS ISO |BS EN ISO |IEEE [A-Z]?)[0-9]+((\.|-)[0-9]+)? ((\.|-)[0-9]+)?(:[0-9]{4})?"]
    (find-citations-like file-or-dir patt)))

