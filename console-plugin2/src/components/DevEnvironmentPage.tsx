import * as React from 'react';
import Helmet from 'react-helmet';
import OperatorStatusLabel from './OperatorStatusLabel';
import NamespaceDropdown from './NamespaceSelect';
import DeploymentInfoTable from './DeploymentInfoTable';
import GiteaResourceDetails from './GiteaResourceDetails';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { Buffer } from 'buffer';
import { Alert, Button, Content, Flex, FlexItem, Form, FormGroup, PageSection, Title, Tab, Tabs, Radio, Select, SelectList, SelectOption, MenuToggle, MenuToggleElement, TextInput, FormHelperText, HelperText, HelperTextItem } from '@patternfly/react-core';
import { CodeEditor, Language } from '@patternfly/react-code-editor';
import { CheckCircleIcon, BanIcon } from '@patternfly/react-icons';

import {
  useActiveNamespace,
  k8sCreate,
  k8sGet,
  k8sUpdate,
  k8sList,
  K8sResourceCommon
} from '@openshift-console/dynamic-plugin-sdk';
import './example.css';
export default function DevEnvironment() {
  const { t } = useTranslation('plugin__console-plugin-template');
  const [activeNamespace] = useActiveNamespace();
  const [activeKey, setActiveKey] = React.useState<number>(0);

  const DevEnvironmentModel = {
    apiVersion: 'v1alpha1',
    apiGroup: 'devjoy.io',
    kind: 'DevEnvironment',
    plural: 'devenvironments',
    label: 'DevEnvironment',
    labelPlural: 'DevEnvironments',
    abbr: 'DE',
    namespaced: true,
  };

  type DevEnvironmentKind = {
    spec: {
      mavenSettingsPvc: string
      gitea: { 
        resourceName: string
        managed: boolean
        enabled: boolean
      }
    };
    status?: {
      gitea: {

      }
    };
  } & K8sResourceCommon;
  const [resourceName, setResourceName] = React.useState('');
  const [giteaEnabled, setGiteaEnabled] = React.useState(false);
  const [giteaManaged, setGiteaManaged] = React.useState(false);
  const [giteaResourceName, setGiteaResourceName] = React.useState<string | null>(null);
  const [mavenSettingsPvc, setMavenSettingsPvc] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [existingResource, setExistingResource] = React.useState<any | null>(null);
  const [loading, setLoading] = React.useState(false);
  
  React.useEffect(() => {
    const loadExisting = async () => {
      if (!activeNamespace) return;
      try {
        const list = await k8sList<DevEnvironmentKind>({ model: DevEnvironmentModel, queryParams: { namespace: activeNamespace }});
        var resource;
        if ('items' in list && list.items?.length === 1) {
          resource = list.items[0]; // erste Ressource verwenden
          
        } else if (list != null) {
          resource = list[0];
        } 
        setExistingResource(resource);
        setResourceName(resource.metadata?.name);
        setGiteaEnabled(resource.spec?.gitea?.enabled || false);
        setGiteaManaged(resource.spec?.gitea?.managed || false);
        setGiteaResourceName(resource.spec?.gitea?.resourceName || '');
        setMavenSettingsPvc(resource.spec?.mavenSettingsPvc || '');
        if (resource.spec?.gitea?.enabled) {
          setSelectedScm("Gitea");
        }
        setNewScm(resource.spec?.gitea?.managed || false);
      } catch (err: any) {
        console.warn('Error loading DevEnvironment:', err);
      } finally {
        setLoading(false);
      }
    };

    loadExisting();
  }, [activeNamespace]);

  const handleSubmit = async () => {
    setError(null);
    const cr = {
      apiVersion: 'devjoy.io/v1alpha1',
      kind: 'DevEnvironment',
      metadata: {
        name: resourceName,
        namespace: activeNamespace,
      },
      spec: {
        gitea: {
          enabled: giteaEnabled,
          managed: giteaManaged,
          resourceName: giteaResourceName,
        },
        mavenSettingsPvc,
      },
    };

    try {
      await k8sCreate({ model: DevEnvironmentModel, data: cr });
      
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };




  const SecretModel = {
    apiVersion: 'v1',
    kind: 'Secret',
    plural: 'secrets',
    label: 'Secret',
    labelPlural: 'Secrets',
    abbr: 'S',
    namespaced: true,
  };
  const handleTabClick = (event: React.MouseEvent<HTMLElement>, tabIndex: number) => {
    setActiveKey(tabIndex);
  };

  const [isOpen, setIsOpen] = useState(false);
  const [selectedScm, setSelectedScm] = useState<string>('Select SCM');
  const [newScm, setNewScm] = useState<boolean>(false);
  const [isDisabled] = useState<boolean>(false);
  const [gitDomain, setGitDomain] = useState('');
  const [gitOrg, setGitOrg] = useState('');
  const [appId, setAppId] = React.useState('');
  const [clientId, setClientId] = React.useState('');
  const [clientSecret, setClientSecret] = React.useState('');
  const [webHookUrl, setWebHookUrl] = React.useState('');
  const [webHookSecret, setWebHookSecret] = React.useState('');
  
  const secretYaml = `
apiVersion: v1
kind: Secret
metadata:
  name: git-server-secret
type: Opaque
data:
  GIT_DOMAIN: ${base64(gitDomain)}
  APP_ID: ${base64(appId)}
  CLIENT_ID: ${base64(clientId)}
  CLIENT_SECRET: ${base64(clientSecret)}
  WEB_HOOK_URL: ${base64(webHookUrl)}
  WEB_HOOK_SECRET: ${base64(webHookSecret)}
`.trim();

const handleCreateSecret = async () => {
  const secret = {
    apiVersion: 'v1',
    kind: 'Secret',
    metadata: {
      name: "git-server-secret",
      namespace: activeNamespace,
    },
    type: 'Opaque',
    data: {
      GIT_DOMAIN: base64(gitDomain),
      APP_ID: base64(appId),
      CLIENT_ID: base64(clientId),
      CLIENT_SECRET: base64(clientSecret),
      WEB_HOOK_URL: base64(webHookUrl),
      WEB_HOOK_SECRET: base64(webHookSecret),
    },
  };

  try {
    await k8sCreate({ model: SecretModel, data: secret });
  } catch (err) {
    console.error('Failed to create secret', err);
  }
};

function base64(value: string) {
  return Buffer.from(value || '', 'utf-8').toString('base64');;
}

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, value: string | number | undefined) => {
    // eslint-disable-next-line no-console
    console.log('selected', value);
    setSelectedScm(value as string);
    setGiteaEnabled(value as string == 'Gitea');
    if (newScm && value as string != 'Gitea') {
      handleConnectScmChange();
    }
    setGiteaManaged(value as string == 'Gitea' && newScm);
    setIsOpen(false);
  };
  const handleNewScmChange = () => {
    setNewScm(true);
    setGiteaManaged(selectedScm == 'Gitea');
  };

  const handleConnectScmChange = () => {
    setNewScm(false);
    setGiteaManaged(false);
  };
 

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isOpen}
      isDisabled={isDisabled}
      style={
        {
          width: '200px'
        } as React.CSSProperties
      }
    >
      {selectedScm}
    </MenuToggle>
  );


  return (
    <>
      <Helmet>
        <title data-test="example-page-title">{t('Hello, Plugin!')}</title>
      </Helmet>
      <PageSection>
        <Title headingLevel="h1">{t('Dev Environments')}</Title>
      </PageSection>
      <PageSection>
        <Content component="p">
          <span className="console-plugin-template__nice">
            <CheckCircleIcon /> {t('Success!')}
          </span>{' '}
          {t('Your plugin is working.')}
        </Content>
        <Content component="p">
          {t(
            'This is a custom page contributed by the console plugin template. The extension that adds the page is declared in console-extensions.json in the project root along with the corresponding nav item. Update console-extensions.json to change or add extensions. Code references in console-extensions.json must have a corresponding property',
          )}
          <code>{t('exposedModules')}</code>{' '}
          {t('in package.json mapping the reference to the module.')}
        </Content>
        <Content component="p">
          {t('After cloning this project, replace references to')}{' '}
          <code>{t('console-template-plugin')}</code>{' '}
          {t('and other plugin metadata in package.json with values for your plugin.')}

          {error && <Alert variant="danger" title="Error creating DevEnvironment" isInline>{error}</Alert>}
          <Form>
            <NamespaceDropdown
              includeAllNamespaces={true}
              labelText="Select namespace"
            />
            <FormGroup label="Name" isRequired fieldId="name">
              <TextInput
                isRequired
                type="text"
                id="name"
                value={resourceName}
                isDisabled= {existingResource}
                onChange={(_, val) => setResourceName(val)}
              />
            </FormGroup>
            <FormGroup>
              <Button variant="primary" onClick={handleSubmit}>
                {existingResource ? 'Update' : 'Create'}
              </Button>
            </FormGroup>
          </Form>
        </Content>
        <Content component="p">
          <Tabs activeKey={activeKey} onSelect={handleTabClick} isBox>
            <Tab eventKey={0} title="Source Control">
              <div style={{ marginTop: '1rem' }}>
              {!existingResource &&(
                  <Form>
                    <FormGroup
                      label="Step 1: Choose your Source-Control-System:">

                    <Select
                      id="scm-select"
                      isOpen={isOpen}
                      selected={selectedScm}
                      onSelect={onSelect}
                      onOpenChange={(isOpen) => setIsOpen(isOpen)}
                      toggle={toggle}
                      shouldFocusToggleOnSelect
                    >
                      <SelectList>
                        <SelectOption value="Gitea">Gitea<OperatorStatusLabel operatorName="gitea-operator" label="Gitea Status" /></SelectOption>
                        <SelectOption value="GitHub">GitHub</SelectOption>
                      </SelectList>
                    </Select>
                    </FormGroup>
                    <FormGroup
                      label="Step 2: Create new or connect existing">
                        <Radio id="new-scm" label="Create new" name="radio-8" description="This will create a new Git server managed by DevJoy" isDisabled={selectedScm != 'Gitea'} isChecked={newScm} onChange={handleNewScmChange}/>
                        <Radio id="connect-scm" label="Connect to existing" name="radio-8" description="This will connect to an existing Git server." isChecked={selectedScm != 'Gitea' || !newScm} onChange={handleConnectScmChange}/>
                    </FormGroup>
                    {!newScm && (
                        <FormGroup 
                        label="Step 3: Provide configuration details">
                          <Flex alignItems={{ default: 'alignItemsStretch' }}>
                            <FlexItem flex={{ default: 'flex_1' }}>
                              <TextInput
                                value={gitDomain}
                                onChange={(_, value) => setGitDomain(value)}
                                isRequired
                                type="text"
                                id="gitDomain"
                                aria-describedby="horizontal-form-name-helper"
                                name="horizontal-form-name"
                              />
                              <FormHelperText>
                                <HelperText>
                                  <HelperTextItem>The domain of your git server, e.g. github.com</HelperTextItem>
                                </HelperText>
                              </FormHelperText>
                              <TextInput
                                value={gitOrg}
                                onChange={(_, value) => setGitOrg(value)}
                                type="text"
                                id="gitOrg"
                                aria-describedby="horizontal-form-name-helper"
                                name="horizontal-form-name"
                              />
                              <FormHelperText>
                                <HelperText>
                                  <HelperTextItem>The organization name</HelperTextItem>
                                </HelperText>
                              </FormHelperText>
                              <TextInput
                              value={appId}
                              onChange={(_, value) => setAppId(value)}
                              isRequired
                              type="text"
                              id="appId"
                              aria-describedby="appId-helper"
                              name="appId"
                            />
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>Your <a href="https://github.com/settings/apps/">Git App ID</a></HelperTextItem>
                              </HelperText>
                            </FormHelperText>

                            <TextInput
                              value={clientId}
                              onChange={(_, value) => setClientId(value)}
                              isRequired
                              type="text"
                              id="clientId"
                              aria-describedby="clientId-helper"
                              name="clientId"
                            />
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>The <a href={'https://github.com/settings/apps/'} target='_github'>OAuth Client ID</a></HelperTextItem>
                              </HelperText>
                            </FormHelperText>

                            <TextInput
                              value={clientSecret}
                              onChange={(_, value) => setClientSecret(value)}
                              isRequired
                              type="password"
                              id="clientSecret"
                              aria-describedby="clientSecret-helper"
                              name="clientSecret"
                            />
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>The OAuth Client Secret</HelperTextItem>
                              </HelperText>
                            </FormHelperText>
                            <TextInput
                              value={webHookUrl}
                              onChange={(_, value) => setWebHookUrl(value)}
                              isRequired
                              type="text"
                              id="webHookUrl"
                              aria-describedby="webHookUrl-helper"
                              name="webHookUrl"
                            />
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>The Webhook callback URL</HelperTextItem>
                              </HelperText>
                            </FormHelperText>
                            <TextInput
                              value={webHookSecret}
                              onChange={(_, value) => setWebHookSecret(value)}
                              isRequired
                              type="password"
                              id="webHookSecret"
                              aria-describedby="webHookSecret-helper"
                              name="webHookSecret"
                            />
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>The Webhook secret used to verify incoming payloads</HelperTextItem>
                              </HelperText>
                            </FormHelperText>
                            <div style={{ marginTop: '1rem' }}>
                              <Button variant="primary" onClick={handleCreateSecret}>
                                Save connection
                              </Button>
                            </div>
                          </FlexItem>
                          <FlexItem flex={{ default: 'flex_1' }} style={{ marginLeft: '2rem' }}>
                            <Title headingLevel="h2">Generated Kubernetes Secret YAML</Title>
                            <CodeEditor
                              isReadOnly
                              isLineNumbersVisible
                              code={secretYaml}
                              language={Language.yaml}
                              height="400px"
                            />
                          </FlexItem>
                        </Flex>
                      </FormGroup>
                    )}
                    
                  </Form>
                  )}
                   {existingResource &&(
                    <p>
                      <GiteaResourceDetails name={existingResource.status?.gitea?.resourceName} namespace={activeNamespace}/>
                      <DeploymentInfoTable names={[existingResource.status?.gitea?.resourceName, 'postgresql-' + existingResource.status?.gitea?.resourceName]} />
                    </p>
                   )}
              </div>
            </Tab>
            <Tab eventKey={1} title="Identity Management">
              <p>Konfiguriere Benutzer, Rollen und Zugriffskontrollen.</p>

            </Tab>
            <Tab eventKey={3} title="Deployment">
              <p>Überwache Deployments, Strategien und Umgebungen.</p>
            </Tab>
            <Tab eventKey={2} title="Build">
              <p>Verwalte Build-Konfigurationen, Pipelines und Artefakte.</p>
            </Tab>
            <Tab eventKey={4} title="API Management">
              <p>Manage deine APIs.</p>
            </Tab>
            <Tab eventKey={5} title="Developer Portal">
              <p>IdP.</p>
            </Tab>
          </Tabs>
        </Content>
      </PageSection>
    </>
  );
}
