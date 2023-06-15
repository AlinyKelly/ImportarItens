var _typeof = function (obj) {
  "@swc/helpers - typeof";
  return obj && typeof Symbol !== "undefined" && obj.constructor === Symbol
    ? "symbol"
    : typeof obj;
};
function getProperty(json, field) {
  var finalValue = {
    data: {},
    type: "object",
  };
  json = JSON.parse(json);
  if (json == null || field == null) {
    return null;
  }
  var value = json;
  var fields = field.split(".");
  for (var i = 0; i < fields.length; i++) {
    value = value[fields[i]];
    if (value == null) {
      finalValue.data = null;
      finalValue.type = "object";
      return JSON.stringify(finalValue);
    }
  }
  if (Array.isArray(value)) {
    finalValue.data = value;
    finalValue.type = "array";
    return JSON.stringify(finalValue);
  }
  finalValue.data = value;
  finalValue.type = typeof value === "undefined" ? "undefined" : _typeof(value);
  return JSON.stringify(finalValue);
}
