import * as React from 'react';
import {
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Label,
  Title,
  Divider,
  Spinner,
  Alert,
} from '@patternfly/react-core';
import { CubeIcon } from '@patternfly/react-icons';
import { k8sGet } from '@openshift-console/dynamic-plugin-sdk';

const GiteaModel = {
  apiVersion: 'v1alpha1',
  apiGroup: 'devjoy.io',
  kind: 'Gitea',
  label: 'Gitea',
  labelPlural: 'Giteas',
  plural: 'giteas',
  abbr: 'GT',
  namespaced: true,
};

type Props = {
  name: string;
  namespace: string;
};

const GiteaResourceDetails: React.FC<Props> = ({ name, namespace }) => {
  const [resource, setResource] = React.useState<any | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    const fetchResource = async () => {
      try {
        const res = await k8sGet({ model: GiteaModel, name, ns: namespace });
        setResource(res);
      } catch (err: any) {
        setError(err.message || 'Error loading Gitea resource');
      } finally {
        setLoading(false);
      }
    };
    fetchResource();
  }, [name, namespace]);

  if (loading) return <Spinner size="xl" />;
  if (error) return <Alert variant="danger" title="Fehler beim Laden der Gitea-Ressource" isInline>{error}</Alert>;
  if (!resource) return null;

  const { metadata, spec, status } = resource;
  const condition = status?.conditions?.[0];

  return (
    <Card isCompact>
      <CardTitle>
        <CubeIcon /> Gitea Instance: {metadata.name}
      </CardTitle>
      <CardBody>
        <DescriptionList isHorizontal>
          <DescriptionListGroup>
            <DescriptionListTerm>Namespace</DescriptionListTerm>
            <DescriptionListDescription>{metadata.namespace}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Created</DescriptionListTerm>
            <DescriptionListDescription>
              {metadata.creationTimestamp
                ? new Date(metadata.creationTimestamp).toLocaleString()
                : '—'}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Image</DescriptionListTerm>
            <DescriptionListDescription>{spec.image || '—'}{spec.imageTag || '—'}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Replicas</DescriptionListTerm>
            <DescriptionListDescription>{spec.replicas ?? '—'}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Admin User</DescriptionListTerm>
            <DescriptionListDescription>{spec.adminUser || '—'}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Admin Email</DescriptionListTerm>
            <DescriptionListDescription>{spec.adminEmail || '—'}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Host</DescriptionListTerm>
            <DescriptionListDescription>
              <a href={'https://' + status?.host || ''} target="_gitea">{status?.host || '—'}</a>
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Root URL</DescriptionListTerm>
            <DescriptionListDescription>
              {spec.giteaConfig?.server?.ROOT_URL || '—'}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>

        <Divider style={{ margin: '1rem 0' }} />

        <Title headingLevel="h4">Status</Title>
        {condition ? (
          <DescriptionList isHorizontal>
            <DescriptionListGroup>
              <DescriptionListTerm>Type</DescriptionListTerm>
              <DescriptionListDescription>{condition.type}</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Status</DescriptionListTerm>
              <DescriptionListDescription>
                <Label
                  color={
                    condition.status === 'True'
                      ? 'green'
                      : condition.status === 'False'
                      ? 'red'
                      : 'orange'
                  }
                >
                  {condition.status}
                </Label>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Reason</DescriptionListTerm>
              <DescriptionListDescription>{condition.reason || '—'}</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Message</DescriptionListTerm>
              <DescriptionListDescription>{condition.message || '—'}</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Last Transition</DescriptionListTerm>
              <DescriptionListDescription>
                {condition.lastTransitionTime
                  ? new Date(condition.lastTransitionTime).toLocaleString()
                  : '—'}
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        ) : (
          <p>Keine Statusinformationen verfügbar.</p>
        )}
      </CardBody>
    </Card>
  );
};

export default GiteaResourceDetails;
