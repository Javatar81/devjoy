import {K8sResourceCommon } from '@openshift-console/dynamic-plugin-sdk';

export enum K8sResourceConditionStatus {
    True = 'True',
    False = 'False',
    Unknown = 'Unknown',
  }
  
  export type K8sResourceCondition = {
    type: string;
    status: keyof typeof K8sResourceConditionStatus;
    lastTransitionTime?: string;
    reason?: string;
    message?: string;
  };


export type GiteaConfigSpec = {    
    enabled?: boolean;
    managed?: boolean;
    resourceName?: string;
  };
  
export type DevEnvironmentKind = {
  spec: {
    gitea?: GiteaConfigSpec;
  };
  status?: {
    conditions?: K8sResourceCondition[];
  };
} & K8sResourceCommon;

export type ProjectKind = {
  spec: {
    environmentName: string
    environmentNamespace: string
    owner: {
      user: string,
      userEmail: string
    }
    quarkus?: {
      enabled?: boolean,
      extensions?: string[]
    }
  };
  status?: {
    conditions?: K8sResourceCondition[];
  };
} & K8sResourceCommon;
