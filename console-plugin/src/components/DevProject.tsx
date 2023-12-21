import * as React from 'react';
import { useEffect, useState } from 'react';
import { Form, FormGroup, TextInput, Popover, ActionGroup, Button } from '@patternfly/react-core';
import { Card, CardTitle, CardBody, CardFooter } from '@patternfly/react-core';
import { Switch } from '@patternfly/react-core';
import { Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';
import { Select, SelectVariant, SelectOption } from '@patternfly/react-core';
import { DualListSelector } from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import { k8sCreate, useK8sModel, k8sList, K8sResourceCommon } from '@openshift-console/dynamic-plugin-sdk';
import { ProjectKind } from '../k8s/types';



export default function DevProject() {

    // K8S Models

    const [projectModel] = useK8sModel({ group: 'devjoy.io', version: 'v1alpha1', kind: 'Project' });
    const [devEnvModel] = useK8sModel({ group: 'devjoy.io', version: 'v1alpha1', kind: 'DevEnvironment' });
    const [ocpProjectModel] = useK8sModel({ group: 'openshift.io', version: 'v1', kind: 'Project' });
    

    // Create K8S resource

    const createProject = () => {
      k8sCreate<ProjectKind>({model: projectModel, data: {
        apiVersion: "devjoy.io/v1alpha1",
        kind: "Project", 
        metadata: {
          name: name,
          namespace: selectedOcpProject
        },
        spec: {
          environmentName: selectedDevEnv,
          environmentNamespace: devEnvOptions.filter(o => o.value == selectedDevEnv).map(o => o.namespace)[0],
          owner: {
            user: "testuser",
            userEmail: "testuser@example.com"
          },
          quarkus: {
            enabled: selectedFramework == "Quarkus",
            extensions: chosenExtensions.map(e => e.toString())
          }
        }
    }}).then(_ => {
        console.log("Patched");
      });
    };
  
    // Project Name
    const [name, setName] = React.useState('');
    
    const handleNameChange = (name: string) => {
      setName(name);
    };

    // OCP Project / Namespace
    const [ocpProjectOptions, setOcpProjectOptions] = useState([]);
    
    useEffect(() => {
      k8sList<K8sResourceCommon>({model: ocpProjectModel, queryParams: {}}).then(projects => {
        setOcpProjectOptions(projects.map(p => 
          {return {
            value: p.metadata.name
          }}
        ));
      });
   }, []);
  
  const [isOcpProjectDropdownOpen, setOcpProjectDropdownOpen] = React.useState(false);
  const [selectedOcpProject, setSelectedOcpProject] = React.useState(null);

  const onOcpProjectToggle = isOpen => {
    setOcpProjectDropdownOpen(true);
  };

  const onOcpProjectSelect = (event, selection, isPlaceholder) => {
    setSelectedOcpProject(selection);
    setOcpProjectDropdownOpen(false);
  };

  // DevEnvironment
  const [devEnvOptions, setDevEnvOptions] = useState([]);
    
  useEffect(() => {
    k8sList<K8sResourceCommon>({model: devEnvModel, queryParams: {}}).then(projects => {
      setDevEnvOptions(projects.map(p => 
        {return {
          value: p.metadata.name,
          namespace: p.metadata.namespace
        }}
      ));
    });
 }, []);

const [isDevEnvDropdownOpen, setDevEnvDropdownOpen] = React.useState(false);
const [selectedDevEnv, setSelectedDevEnv] = React.useState(null);

const onDevEnvToggle = isOpen => {
  setDevEnvDropdownOpen(true);
};

const onDevEnvSelect = (event, selection, isPlaceholder) => {
  setSelectedDevEnv(selection);
  console.log("HEY: " + selection.value)
  setDevEnvDropdownOpen(false);
};
  

  // Repo Url
    
  const [repoUrl, setRepoUrl] = React.useState('');
  
  const handleRepoUrlChange = (repoUrl: string) => {
      setRepoUrl(repoUrl);
  };

  const [isRepoCreateChecked, setIsRepoCreateChecked] = React.useState<boolean>(true);

  const repoCreateChange = (checked: boolean, _event: React.FormEvent<HTMLInputElement>) => {
    setIsRepoCreateChecked(checked);
  };

  // Language

  const [isLanguageOpen, setLanguageIsOpen] = React.useState(false);
  const [language] = React.useState('');

  const onLanguageToggle = (isOpen: boolean) => {
    setLanguageIsOpen(isOpen);
  };

  const onLanguageFocus = () => {
    const element = document.getElementById('toggle-basic');
    element.focus();
  };

  const onLanguageSelect = () => {
    setLanguageIsOpen(false);
    onLanguageFocus();
  };

  const languageDropdownItems = [
    <DropdownItem key="Java17" tooltip="Tooltip for enabled link">
      Java 17
    </DropdownItem>,
    <DropdownItem key="Java21" component="button" tooltip="Tooltip for enabled button">
      Java 21
    </DropdownItem>
  ];

  // Frameworks

  const frameworkOptions = [
    { value: 'Quarkus', disabled: false, description: "Traditional Java stacks were engineered for monolithic applications with long startup times and large memory requirements in a world where the cloud, containers, and Kubernetes did not exist. Java frameworks needed to evolve to meet the needs of this new world."},
    { value: 'Spring Boot', disabled: true, description: "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can just run." }
  ];


  const [isFrameworkDropdownOpen, setFrameworkDropdownOpen] = React.useState(false);
  const [isFrameworkDropdownDisabled] = React.useState(false);
  const [selectedFramework, setSelectedFramework] = React.useState(null);

  const onFrameworkToggle = isOpen => {
    setFrameworkDropdownOpen(true);
  };

  const onFrameworkSelect = (event, selection, isPlaceholder) => {
    if (isPlaceholder) clearFrameworkSelection();
    else {
      setSelectedFramework(selection);
      setFrameworkDropdownOpen(false);
    }
  };

  const clearFrameworkSelection = () => {
    setSelectedFramework(null);
    setFrameworkDropdownOpen(false);
  };

  // Extensions
  const [availableExtensions, setAvailableExtensions] = useState([]);
  const [chosenExtensions, setChosenExtensions] = React.useState<React.ReactNode[]>([]);
  const onExtensionListChange = (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => {
    setAvailableExtensions(newAvailableOptions.sort());
    setChosenExtensions(newChosenOptions.sort());
  };
  //https://editor.swagger.io/?url=https://code.quarkus.io/q/openapi
  useEffect(() => {
    fetch('https://code.quarkus.io/api/extensions?platformOnly=true')
        .then((response) => response.json())
        .then((data) => {
          setAvailableExtensions(data.map(e => e.id));
        })
        .catch((err) => {
          console.log(err.message);
        });
  }, []);

    return (
      <Card>
      <CardTitle>Create a new project</CardTitle>
      <CardBody>
        <Form>
          <FormGroup
            label="Project name"
            labelIcon={
              <Popover
                headerContent={
                  <div>
                    The{' '}
                    <a href="https://schema.org/name" target="_blank" rel="noreferrer">
                      name
                    </a>{' '}
                    of a{' '}
                    <a href="https://schema.org/Project" target="_blank" rel="noreferrer">
                      Project
                    </a>
                  </div>
                }
                bodyContent={
                  <div>
                   The name of a project such as mynewapp.
                  </div>
                }
              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={e => e.preventDefault()}
                  aria-describedby="simple-form-name-01"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            }
            isRequired
            fieldId="simple-form-name-01"
            helperText="Include your middle name if you have one."
          >
            <TextInput
              isRequired
              type="text"
              id="simple-form-name-01"
              name="simple-form-name-01"
              aria-describedby="simple-form-name-01-helper"
              value={name}
              onChange={handleNameChange}
            />
          </FormGroup>
          <FormGroup label="OpenShift Project" isRequired fieldId="simple-form-ocp-project-01">
            <Select 
              variant={SelectVariant.single}
              placeholderText="Select an option"
              aria-label="Select Input with descriptions"
              onToggle={onOcpProjectToggle}
              onSelect={onOcpProjectSelect}
              selections={selectedOcpProject}
              isOpen={isOcpProjectDropdownOpen}
              aria-labelledby="'select-ocp-project-title"
            >
             {ocpProjectOptions.map((option, index) => (
            <SelectOption
              key={index}
              value={option.value}
            />
          ))}
            </Select>
          </FormGroup>
          <FormGroup label="Development Environment" isRequired fieldId="simple-form-dev-env-01">
            <Select 
              variant={SelectVariant.single}
              placeholderText="Select an option"
              aria-label="Select Input with descriptions"
              onToggle={onDevEnvToggle}
              onSelect={onDevEnvSelect}
              selections={selectedDevEnv}
              isOpen={isDevEnvDropdownOpen}
              aria-labelledby="'select-dev-env-title"
            >
             {devEnvOptions.map((option, index) => (
            <SelectOption
              key={index}
              value={option.value}
            />
          ))}
            </Select>
          </FormGroup>
          <FormGroup
            label="Source code repository"
            labelIcon={
              <Popover
                headerContent={
                  <div>
                    The{' '}
                    <a href="https://schema.org/name" target="_blank" rel="noreferrer">
                      name
                    </a>{' '}
                    of a{' '}
                    <a href="https://schema.org/Project" target="_blank" rel="noreferrer">
                      Project
                    </a>
                  </div>
                }
                bodyContent={
                  <div>
                   The name of a project such as mynewapp.
                  </div>
                }
              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={e => e.preventDefault()}
                  aria-describedby="simple-form-name-01"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            }
            isRequired
            fieldId="simple-form-name-01"
            helperText="Choose if you want to create a new repository for your project or you have already created one."
          >
            <Switch
              id="simple-switch"
              label="Create a repository for me"
              labelOff="I've already created a repository"
              isChecked={isRepoCreateChecked}
              onChange={repoCreateChange}
            />
          </FormGroup>
          <FormGroup label="Repository Url" isRequired fieldId="simple-form-email-01" hidden={isRepoCreateChecked}>
            <TextInput
              isRequired
              type="text"
              id="repo-url-01"
              name="repo-url-01"
              aria-describedby="repo-url-01-helper"
              value={repoUrl}
              onChange={handleRepoUrlChange}
            />
          </FormGroup>
          <FormGroup label="Language" isRequired fieldId="simple-form-language-01">
            <Dropdown
                onSelect={onLanguageSelect}
                toggle={
                  <DropdownToggle id="toggle-primary" onToggle={onLanguageToggle}>
                    Java 17
                  </DropdownToggle>
                }
                isOpen={isLanguageOpen}
                dropdownItems={languageDropdownItems}
                value={language}
              />
          </FormGroup>

          <FormGroup label="Framework" isRequired fieldId="simple-form-framework-01">
            <Select 
              variant={SelectVariant.single}
              placeholderText="Select an option"
              aria-label="Select Input with descriptions"
              onToggle={onFrameworkToggle}
              onSelect={onFrameworkSelect}
              selections={selectedFramework}
              isOpen={isFrameworkDropdownOpen}
              aria-labelledby="'select-framework-title"
              isDisabled={isFrameworkDropdownDisabled}
            >
             {frameworkOptions.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              description={option.description}
            />
          ))}
            </Select>
          </FormGroup>
          <FormGroup label={selectedFramework == "Quarkus" ? "Extensions" : "Dependencies"} fieldId="simple-form-deps-01">
          <DualListSelector
            isSearchable
            availableOptions={availableExtensions}
            chosenOptions={chosenExtensions}
            onListChange={onExtensionListChange}
            id="dual-list-selector-basic-search"
          />


          </FormGroup>
          <ActionGroup>
            <Button variant="primary" onClick={createProject}>Submit</Button>
            <Button variant="link">Cancel</Button>
          </ActionGroup>
        </Form>
      </CardBody>
      <CardFooter>Footer</CardFooter>
    </Card>
  );
}