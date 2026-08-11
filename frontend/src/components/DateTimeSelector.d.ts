export function DateTimeSelector(props: {
  value: Date;
  onChange: (value: Date) => void;
  minimumDate?: Date;
  maximumDate?: Date;
  dateOnly?: boolean;
  label?: string;
}): import("react").ReactElement;
