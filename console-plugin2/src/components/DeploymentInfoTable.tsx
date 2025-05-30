import * as React from 'react';
import {
    Card,
    CardBody,
    CardTitle,
    Spinner,
    Bullseye,
    Alert,
} from '@patternfly/react-core';
import {
    Table,
    Thead,
    Tr,
    Th,
    Tbody,
    Td,
    Caption,
  } from '@patternfly/react-table';

import { CubeIcon } from '@patternfly/react-icons';
import { k8sGet, useActiveNamespace } from '@openshift-console/dynamic-plugin-sdk';

// Minimalmodell des Deployment-Objekts
const DeploymentModel = {
  apiVersion: 'v1',
  apiGroup: 'apps',
  kind: 'Deployment',
  plural: 'deployments',
  label: 'Deployment',
  labelPlural: 'Deployments',
  abbr: 'D',
  namespaced: true,
};

type Props = {
  names: string[]; // Name des Deployments
};

const DeploymentInfoTable: React.FC<Props> = ({ names }) => {
  const [namespace] = useActiveNamespace();
  const [deployments, setDeployments] = React.useState<any[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    const fetchAll = async () => {
      setLoading(true);
      setError(null);
      try {
        const results = await Promise.all(
          names.map(name =>
            k8sGet({ model: DeploymentModel, name, ns: namespace })
          )
        );
        setDeployments(results);
      } catch (err: any) {
        console.error(err);
        setError('Fehler beim Laden der Deployments.');
      } finally {
        setLoading(false);
      }
    };

    fetchAll();
  }, [names, namespace]);

  if (loading) {
    return (
      <Bullseye>
        <Spinner size="xl" />
      </Bullseye>
    );
  }

  if (error) {
    return (
      <Alert variant="danger" title="Deployment Load Error">
        {error}
      </Alert>
    );
  }

  return (
    <Card isCompact>
      <CardTitle>
        <CubeIcon /> <a href={'k8s/ns/' + namespace + '/apps~v1~Deployment'} target="_deployments">Deployments</a> related to Gitea 
      </CardTitle> 
      <CardBody></CardBody>
    <Table aria-label="Deployment Details" variant="compact">
      <Thead>
        <Tr>
          <Th>Name</Th>
          <Th>Namespace</Th>
          <Th>Replicas</Th>
          <Th>Available</Th>
          <Th>Status</Th>
        </Tr>
      </Thead>
      <Tbody>
        {deployments.map((dep) => (
          <Tr key={dep.metadata.uid}>
            <Td>{dep.metadata.name}</Td>
            <Td>{dep.metadata.namespace}</Td>
            <Td>{dep.spec?.replicas ?? '—'}</Td>
            <Td>{dep.status?.availableReplicas ?? 0}</Td>
            <Td>
              {dep.status?.availableReplicas === dep.spec?.replicas
                ? 'Running'
                : 'Pending'}
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
    </Card>
  );
};

export default DeploymentInfoTable;
