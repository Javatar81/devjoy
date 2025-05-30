import * as React from 'react';
import {
  Select,
  SelectOption,
  Spinner,
  Button,
  FormGroup,
  MenuToggle,
  MenuToggleElement
} from '@patternfly/react-core';
import { useState } from 'react';
import {
  useK8sWatchResource,
  useActiveNamespace,
} from '@openshift-console/dynamic-plugin-sdk';

type NamespaceDropdownProps = {
  includeAllNamespaces?: boolean;
  labelText?: string;
};

const NamespaceDropdown: React.FC<NamespaceDropdownProps> = ({
  includeAllNamespaces = true,
  labelText = 'Namespace auswählen',
}) => {
  const [activeNamespace, setNamespace] = useActiveNamespace(); // z. B. 'default', 'my-app', 'ALL_NAMESPACES'
  const [isOpen, setIsOpen] = React.useState(false);
  const [selected, setSelected] = React.useState<string | null>(null);
  const [isDisabled] = useState<boolean>(false);

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const onSelectNamespace = (namespace) => {
    setNamespace(namespace);
  };
  const [projects, loaded, error] = useK8sWatchResource<any[]>({
    groupVersionKind: {
      group: 'project.openshift.io',
      version: 'v1',
      kind: 'Project',
    },
    isList: true,
  });

  React.useEffect(() => {
    if (!selected && activeNamespace) {
      setSelected(activeNamespace);
      onSelectNamespace(activeNamespace);
    }
  }, [activeNamespace, selected, onSelectNamespace]);

  const namespaceOptions = React.useMemo(() => {
    if (!loaded || error || !Array.isArray(projects)) return [];
    const names = projects.map((project) => project.metadata.name).sort();
    return includeAllNamespaces ? ['ALL_NAMESPACES', ...names] : names;
  }, [projects, loaded, error, includeAllNamespaces]);

  const onSelect = (event, value: string) => {
    setSelected(value);
    onSelectNamespace(value);
    setIsOpen(false);
  };

  const onReset = () => {
    setSelected(activeNamespace);
    onSelectNamespace(activeNamespace);
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
      {selected}
    </MenuToggle>
  );

  if (error) {
    return <div>Fehler beim Laden der Namespaces.</div>;
  }

  if (!loaded) {
    return <Spinner size="md" />;
  }

  return (
    <FormGroup label={labelText} fieldId="namespace-select">
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <Select
          onSelect={onSelect}
          toggle={toggle}
          isOpen={isOpen}
        >
          {namespaceOptions.map((ns) => (
            <SelectOption key={ns} value={ns}>
              {ns === 'ALL_NAMESPACES' ? 'Alle Namespaces' : ns}
            </SelectOption>
          ))}
        </Select>
        <Button variant="secondary" onClick={onReset}>
          Reset
        </Button>
      </div>
    </FormGroup>
  );
};

export default NamespaceDropdown;