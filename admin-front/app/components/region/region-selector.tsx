import { Cascader } from "antd";
import { useState, useEffect } from "react";
import {
  getRegionsByLevel,
  getRegionsByParent,
} from "~/apis/https/region/region.admin";

interface RegionSelectorProps {
  value?: number;
  onChange?: (val: number) => void;
}

const RegionSelector: React.FC<RegionSelectorProps> = ({ value, onChange }) => {
  const [options, setOptions] = useState<any[]>([]);
  const [valuePath, setValuePath] = useState<number[]>([]);

  // ✅ 当外部传来的 value 变化时，清空路径（否则 ProTable 表单不会更新）
  useEffect(() => {
    if (!value) {
      setValuePath([]);
    }
  }, [value]);

  // 初始加载省级
  useEffect(() => {
    (async () => {
      const provinces = await getRegionsByLevel("province");
      setOptions(
        provinces.map((p) => ({
          label: p.regionName,
          value: p.adcode,
          isLeaf: false,
        })),
      );
    })();
  }, []);

  // 动态加载市/区
  const loadData = async (selectedOptions: any[]) => {
    const targetOption = selectedOptions[selectedOptions.length - 1];
    targetOption.loading = true;

    const children = await getRegionsByParent(targetOption.value);
    targetOption.loading = false;

    targetOption.children = children.map((c) => ({
      label: c.regionName,
      value: c.adcode,
      isLeaf: c.regionLevel === "district",
    }));

    setOptions([...options]);
  };

  return (
    <Cascader
      options={options}
      value={valuePath}
      loadData={loadData}
      onChange={(vals) => {
        setValuePath(vals);
        onChange?.(vals[vals.length - 1]); // ✅ 回传最终 adcode 给 ProTable
      }}
      changeOnSelect
      placeholder="请选择行政区"
      allowClear
    />
  );
};

export default RegionSelector;
