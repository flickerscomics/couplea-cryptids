(ns couplea.macros
  "Compile-time macros that read site.edn / stock.edn and embed them in the
   JavaScript. A mistake in either file stops the build with a plain message."
  (:require [couplea.validate :as v])
  (:import (java.time LocalDate ZoneId)
           (java.time.format DateTimeFormatter)
           (java.util Locale)))

(defn- loaded []
  (let [{:keys [errors] :as r} (v/load-all)]
    (when (seq errors)
      (throw (ex-info (v/format-errors errors) {})))
    r))

(defmacro site-config
  "site.edn as a literal map."
  []
  (:site (loaded)))

(defmacro stock-config
  "stock.edn as a literal vector."
  []
  (:stock (loaded)))

(defmacro build-date
  "Today's date when the site was built, like \"20 Oct 2026\"."
  []
  (.format (LocalDate/now (ZoneId/of "UTC"))
           (DateTimeFormatter/ofPattern "d MMM yyyy" Locale/ENGLISH)))
