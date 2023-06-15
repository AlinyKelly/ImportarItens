function getKeys(json) {
  var finalValue = {};
  finalValue.data = null;
  finalValue.type = "object";
  if (typeof json == "string")
    return JSON.stringify({
      data: Object.keys(JSON.parse(json)),
      type: "array",
    });
  if (Array.isArray(json) || typeof json === "object")
    return JSON.stringify({
      data: Object.keys(json),
      type: "array",
    });
  return JSON.stringify({
    data: [],
    type: "array",
  });
}
