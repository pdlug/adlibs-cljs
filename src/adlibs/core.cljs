(ns adlibs.core
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [reagent.dom :as reagent-dom]))

(def sample-text
  ["Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
   :place "eiusmod tempor incididunt ut labore et dolore magna aliqua.
   Ut enim ad minim veniam, quis " :name " nostrud exercitation ullamco "
   "laboris nisi ut aliquip ex ea " :thing "commodo consequat. Duis aute "
   "irure dolor in reprehenderit in " :city " voluptate velit esse cillum "
   "dolore eu fugiat nulla pariatur."])

(defn text-input
  [value & {:keys [on-enter on-up on-down placeholder ref]
            :or   {on-enter    identity
                   on-up       identity
                   on-down     identity
                   ref         identity
                   placeholder ""}}]
  [:input {:type          "text"
           :value         @value
           :placeholder   placeholder
           :aria-label    placeholder
           :aria-required "true"
           :on-change     #(reset! value (-> % .-target .-value))
           :on-key-down   #(case (.-which %)
                             13 (if-not (string/blank? @value)
                                  (on-enter @value))
                             38 (on-up @value)
                             40 (on-down @value)
                             nil)
           :ref           ref}])

(defn build-component
  [word]
  (let [v   (r/atom "")
        !el (atom nil)]
    {:name      (name word)
     :value     v
     :el        !el
     :component [text-input v
                 :placeholder (name word)
                 :ref #(reset! !el %)]}))

(defn input-map
  "Replace symbol placeholders in text with generated symbols corresponding
   to atoms"
  [text]
  (reduce
   (fn [m word]
     (if (string? word)
       (assoc m :text (conj (:text m) [nil word]))
       (do
         (let [s (gensym)
               c (build-component word)]
           (-> m
               (assoc-in [:inputs s] c)
               (assoc :text (conj (:text m) [s (:component c)]))
               (assoc :order (conj (:order m) s)))))))
   {:inputs {}
    :order  []
    :text   []}
   text))

(defn positions
  "Returns the positions in a collection where the given value appears"
  [v coll]
  (keep-indexed #(when (= %2 v) %1) coll))

(defn set-focus
  "Set the focus to the next or previous input in the sequence"
  [dir current inputs]
  (let [order (:order inputs)
        idx   (first (positions current order))
        next  (get order (if (= dir :next)
                           (inc idx)
                           (dec idx)))]
    (if-let [el (get-in inputs [:inputs next :el])]
      (.focus @el))))

(defn set-focus-to-first
  "Set the focus to the first input"
  [inputs]
  (if-let [el @(get-in inputs [:inputs (first (:order inputs)) :el])]
    (.focus el)))

(defn complete?
  "Whether or not all inputs have been filled in"
  [inputs]
  (not-any? string/blank? (map #(deref (:value %))
                               (vals (:inputs inputs)))))

(defn reset-all
  "Reset all state and start over"
  [inputs reveal?]
  (doseq [i (vals (:inputs inputs))]
    (reset! (:value i) ""))
  (reset! reveal? false)
  (set-focus-to-first inputs))

(defn text-block
  [inputs reveal?]
  (let [on-enter (fn [cur]
                   (if (complete? inputs)
                     (reset! reveal? true)
                     (reset! reveal? false))
                   (set-focus :next cur inputs))]
    (r/create-class
     {:component-did-mount #(set-focus-to-first inputs)
      :display-name        "text-block"
      :reagent-render
      (fn []
        (into [:div
               {:class ["text" (if-not @reveal? "blurry")]}]
              (map (fn [[id c]]
                     (if (string? c)
                       c
                       (conj c
                             :on-enter (partial on-enter id)
                             :on-down #(set-focus :next id inputs)
                             :on-up #(set-focus :prev id inputs))))
                   (:text inputs))))})))

(defn button
  [label & {:keys [className on-click]}]
  [:a
   {:class (str "button " className)
    :on-click  #(-> % .preventDefault on-click)}
   label])

(defn buttons
  [inputs reveal?]
  [:div.buttons
   [button "Reveal" :class "primary" :on-click #(reset! reveal? true)]
   [button "Reset" :class "reset" :on-click #(reset-all inputs reveal?)]])

(defn ad-lib
  [text]
  (let [inputs  (input-map text)
        reveal? (r/atom false)]
    (fn []
      [:div
       {:on-key-down #(if (= (.-which %) 27)
                        (reset-all inputs reveal?))}
       [text-block inputs reveal?]
       (if (complete? inputs)
         [buttons inputs reveal?])])))

(if-let [el (.getElementById js/document "app")]
  (reagent-dom/render [ad-lib sample-text] el))
