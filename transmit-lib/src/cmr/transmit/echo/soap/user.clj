(ns cmr.transmit.echo.soap.user
  "Helper to perform User tasks against the SOAP API."
  (:require [cmr.transmit.echo.soap.core :as soap]
            [cmr.common.xml.parse :as xp]
            [cmr.common.xml.simple-xpath :as xpath]
            [cmr.common.log :refer (debug info warn error)]))

;; keys within a user map.
(def user-keys [:password :guid :user-domain :user-region :primary-study-area :user-type :username :title
                :first-name :middle-initial :last-name :param-map :email :opt-in :organization-name :addresses
                :phones :roles :creation-date])

;; A minimally valid user map
(def minimal-user
  {:user-domain "GOVERNMENT"
   :primary-study-area  "AIR_SEA_INTERACTION" :user-type  "PRODUCTION_USER"
   :first-name  "Admin" :last-name  "User" :email  "admin@example.com"
   :user-region "USA" :organization-name "ECHO"
   :opt-in "false" :addresses [["ns3:Country" "USA"]]})

(defn- create-user-request
  "Returns a hiccup representation of the SOAP body for a CreateUser request using the provided parameters."
  [param-map]
  (let [{:keys [token password guid user-domain user-region primary-study-area user-type username title
                first-name middle-initial last-name param-map email opt-in organization-name addresses
                phones roles creation-date]} param-map]
    ["ns2:CreateUser"
      soap/soap-ns-map
      ["ns2:token" token]
      ["ns2:password" password]
      ["ns2:newUser"
        ;; NOTE the when forms arent really necessary as empty elements will be ommitted when we convert
        ;; to XML anyway, but this makes it easier to see which elements are required and which arent.
        ;;  Ideally we will implement a better approach.
        ["ns3:UserDomain" (or user-domain "OTHER")]
        ["ns3:UserRegion" (or user-region "USA")]
        (when primary-study-area ["ns3:PrimaryStudyArea" primary-study-area])
        (when user-type ["ns3:UserType" user-type])
        ["ns3:Username" username]
        (when title ["ns3:Title" title])
        ["ns3:FirstName" first-name]
        (when middle-initial ["ns3:MiddleInitial" middle-initial])
        ["ns3:LastName" last-name]
        ["ns3:Email" email]
        ["ns3:OptIn" (or opt-in "false")]
        (when organization-name ["ns3:OrganizationName" organization-name])
        ;; For now, addresses, phones, and roles need to be passed in already in hiccup format
        ["ns3:Addresses" (soap/item-list (or addresses [["ns3:Country" "USA"]]))]
        (when phones ["ns3:Phones" (soap/item-list phones)])
        (when roles ["ns3:Roles" (soap/item-list roles)])
        (when creation-date ["ns3:CreationDate" creation-date])]]))

(defn- create-get-user-by-user-name-request
  "Returns a hiccup representation of the SOAP body for a GetUserByUserName request using the provided parameters."
  [param-map]
  (let [{:keys [token user-name]} param-map]
    ["ns2:GetUserByUserName"
      soap/soap-ns-map
      ["ns2:token" token]
      ["ns2:userName" user-name]]))

(defn- create-get-current-user-request
  "Returns a hiccup representation of the SOAP body for a GetCurrentUser request using the provided parameters."
  [token]
  ["ns2:GetCurrentUser"
    soap/soap-ns-map
    ["ns2:token" token]])

(defn create-user
  "Perform a CreateUser request against the SOAP API.  Takes a map containing request parameters:
    [:token :password :guid :user-domain :user-region :primary-study-area :user-type :username :title
     :first-name :middle-initial :last-name :param-map :email :opt-in :organization-name :addresses
     :phones :roles :creation-date]"
  [param-map]
  (soap/string-from-soap-request :user :create-user (create-user-request param-map)))

(defn get-user-by-user-name
  "Perform a GetUserByUserName request against the SOAP API.  Takes a map containing request parameters:
    [:token :user-name]
    Note that this returns a flat map, and does not contain nested elements like addresses.
      TODO: shoudl we fix this?"
  [param-map]
  (soap/item-map-from-soap-request :user :get-user-by-user-name
      (create-get-user-by-user-name-request param-map) user-keys))

(defn get-current-user
  "Perform a GetCurrentUser request against the SOAP API using the provided token."
  [token]
  (soap/item-map-from-soap-request :user :get-current-user
      (create-get-current-user-request token) user-keys))
