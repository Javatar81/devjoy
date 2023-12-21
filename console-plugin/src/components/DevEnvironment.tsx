import * as React from 'react';
import {useK8sWatchResource} from '@openshift-console/dynamic-plugin-sdk';
import { useK8sModel } from '@openshift-console/dynamic-plugin-sdk';
import { DevEnvironmentKind } from '../k8s/types';
import {
  Brand,
  Card,
  CardHeader,
  CardHeaderMain,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';
const envLogo = require('../img/pfLogo.svg');

export default function DevEnvironment() {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [isChecked, setIsChecked] = React.useState<boolean>(false);
  const [hasNoOffset] = React.useState<boolean>(false);

  const onSelect = () => {
    setIsOpen(!isOpen);
  };
  const onClick = (checked: boolean) => {
    setIsChecked(checked);
  };

  const dropdownItems = [
    <DropdownItem key="link">Link</DropdownItem>,
    <DropdownItem key="action" component="button">
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled>
      Disabled Link
    </DropdownItem>,
    <DropdownItem key="disabled action" isDisabled component="button">
      Disabled Action
    </DropdownItem>,
    <DropdownSeparator key="separator" />,
    <DropdownItem key="separated link">Separated Link</DropdownItem>,
    <DropdownItem key="separated action" component="button">
      Separated Action
    </DropdownItem>
  ];

  
  let envs = useK8sWatchResource<DevEnvironmentKind[]>({
    groupVersionKind: {
      group: 'devjoy.io',
      version: 'v1alpha1',
      kind: 'DevEnvironment',
    },
    isList: true,
    namespaced: true,
  });
  /*envs[0].forEach(function (env, index) {
    console.log("ENV")
    console.log(env.metadata.name);
  });*/
  
  const [podModel] = useK8sModel({ group: 'devjoy.io', version: 'v1alpha1', kind: 'DevEnvironment' });

    return (
      console.log("----------models-------"), console.log(podModel), console.log("----------PODS-------"),console.log(envs),
      <React.Fragment>
         {envs[0].map(function (env) {
          return(
            <Card>
            <CardHeader>
              <CardHeaderMain>
                <Brand src={envLogo} alt="PatternFly logo" style={{ height: '50px' }} />
              </CardHeaderMain>
              <CardActions hasNoOffset={hasNoOffset}>
                <Dropdown
                  onSelect={onSelect}
                  toggle={<KebabToggle onToggle={setIsOpen} />}
                  isOpen={isOpen}
                  isPlain
                  dropdownItems={dropdownItems}
                  position={'right'}
                />
                <Checkbox
                  isChecked={isChecked}
                  onChange={onClick}
                  aria-label="card checkbox example"
                  id="check-1"
                  name="check1"
                />
              </CardActions>
            </CardHeader>
            <CardTitle>Development Environment {env.metadata.name}</CardTitle>
            <CardBody>

            </CardBody>
            <CardFooter>Footer</CardFooter>
          </Card>)
        })}
      </React.Fragment>
      
    )
}