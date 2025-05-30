import * as React from 'react';
import { useK8sWatchResource } from '@openshift-console/dynamic-plugin-sdk';
import { Label } from '@patternfly/react-core';
import {
  CheckCircleIcon,
  BanIcon,
  InProgressIcon,
  UnknownIcon,
} from '@patternfly/react-icons';

type OperatorStatusLabelProps = {
  operatorName: string; // z. B. 'keycloak-operator'
  namespace?: string;
  label?: string;
};

const OperatorStatusLabel: React.FC<OperatorStatusLabelProps> = ({
  operatorName,
  namespace,
  label = '',
}) => {
  const [resources, loaded, error] = useK8sWatchResource<any[]>({
    groupVersionKind: {
      group: 'operators.coreos.com',
      version: 'v1alpha1',
      kind: 'ClusterServiceVersion',
    },
    namespace,
    isList: true,
  });

  if (!loaded && !error) {
    return (
      <Label icon={<InProgressIcon />} color="blue">
        Lädt...
      </Label>
    );
  }

  if (error) {
    return (
      <Label icon={<UnknownIcon />} color="red">
        Fehler beim Laden
      </Label>
    );
  }

  const operatorFound = Array.isArray(resources)
    ? resources.find((csv) =>
        csv?.metadata?.name?.toLowerCase().includes(operatorName.toLowerCase())
      )
    : false;

  if (operatorFound) {
    return (
      <Label icon={<CheckCircleIcon />} color="green">
        {label || `${operatorName} installiert`}
      </Label>
    );
  }

  return (
    <Label icon={<BanIcon />} color="grey">
      {label || `${operatorName} nicht installiert`}
    </Label>
  );
};

export default OperatorStatusLabel;
