# Autoscaling of Embedded JDG application in OpenShift

This quickstart demonstrates how an application running with JDG in embedded mode on OpenShift can horizontally scale up as the memory requirements go up. Th

## Prerequisites

1. Availability of OpenShift environment, preferably v3.9+ started with metrics enabled
2. [Autoscaling for Memory Utilization](https://docs.openshift.com/container-platform/3.9/dev_guide/pod_autoscaling.html#pod-autoscaling-memory) is enabled

## Deployment 

1. From command line, run the following command and authenticate with an user account 
  
  ```sh
   oc login https://<Openshift IP>:8443
  ```
2. If no project exists create a new project with the following command: 
  
  ```sh
   oc new-project myproject --display-name="My Project"
  ```
3. Using the **system:admin** account, by logging in as: `oc login -u system:admin`, run the following command to allow access read access to the kubernetes API (please note the project name): 
   
  ```sh
   oc policy add-role-to-user view system:serviceaccount:myproject:default -n myproject 
  ```
4. Log back in with the user account you used in step #1 and then in this project's root folder run the command :
  
  ```
  mvn fabric8:deploy
  ```
5. If verified that a single pod is created in the project/namespace with the name **embedded-jdg-openshift** then run configure the pod autoscaling with the following command. This should scale the pods to a minimum of 3 triggering the data load from the coordinator node and in turn eventually triggering autoscaling.
   
  ```sh
   oc create -f src/main/resources/hpa.yaml
  ```
   
## Verification 

To verify the metrics of the pods run the following command periodically. The pods should autoscale to 4 in about 10-15 mins. 

```sh
oc describe hpa/hpa-resource-metrics-memory
```