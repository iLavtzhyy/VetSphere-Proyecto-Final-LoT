// Entorno de producción: sustituye environment.ts vía fileReplacements en angular.json.
// Ruta RELATIVA: en Docker/producción nginx hace de proxy de /api hacia el backend.
export const environment = {
  apiUrl: '/api',
  iotApiUrl: '/iot/api/iot',
  production: true
};
