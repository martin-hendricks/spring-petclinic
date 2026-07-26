import http from 'k6/http';
import { check, group, sleep } from 'k6';

// Prueba de carga parametrizable — legado (MVC Thymeleaf, sin capa @Service ni API REST)
// vs. modernizado (F-03/F-05). Diseñada para corregir los vicios señalados en la revisión
// metodológica previa (aplicada aquí, aunque esa revisión era sobre otro sistema con
// SNS/SQS): rondas de 20-30s sin calentamiento, sin repeticiones, sin distinguir
// admisión vs. procesamiento completo.
//
// Diferencia relevante con ese caso: PetClinic (legado y modernizado) es 100% síncrono
// (JPA/Hibernate directo a la BD, sin cola ni notificación asíncrona de por medio), así
// que http_req_duration aquí SÍ es el tiempo de procesamiento completo de la petición,
// no solo el de admisión — no aplica la distinción "POST /orders admitido vs. terminado
// por SNS/SQS" de ese otro contexto.
//
// MODE=legado      -> solo endpoints MVC (Thymeleaf), el legado no tiene /api/owners.
// MODE=modernizado -> API REST (F-05) + MVC en paralelo (patrón Wrapping).
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MODE = __ENV.MODE || 'legado';
const TARGET_VUS = parseInt(__ENV.TARGET_VUS || '10', 10);
const RAMP = __ENV.RAMP || '30s';
const STEADY = __ENV.STEADY || '3m';

export const options = {
	scenarios: {
		carga: {
			executor: 'ramping-vus',
			startVUs: 0,
			stages: [
				{ duration: RAMP, target: TARGET_VUS }, // calentamiento: evita medir con
				// conexiones/caches frías o el arranque JIT de la JVM
				{ duration: STEADY, target: TARGET_VUS }, // meseta sostenida: aquí se mide
				{ duration: RAMP, target: 0 }, // enfriamiento
			],
			gracefulRampDown: '30s',
		},
	},
	// Sin thresholds duros: en 250/500 VUs el objetivo es caracterizar el techo del
	// legado (incluyendo fallos y rechazo de conexiones esperados), no aprobar/reprobar.
};

function hitLegado() {
	group('UI Thymeleaf — legado', function () {
		const ownerPage = http.get(`${BASE_URL}/owners/1`, { tags: { endpoint: 'owner_by_id' } });
		check(ownerPage, { 'GET /owners/1 -> 200': (r) => r.status === 200 });

		const ownersList = http.get(`${BASE_URL}/owners?lastName=`, { tags: { endpoint: 'owners_list' } });
		check(ownersList, { 'GET /owners -> 200': (r) => r.status === 200 });
	});
}

function hitModernizado() {
	group('API REST — modernizado', function () {
		const byId = http.get(`${BASE_URL}/api/owners/1`, { tags: { endpoint: 'api_owner_by_id' } });
		check(byId, { 'GET /api/owners/1 -> 200': (r) => r.status === 200 });

		const list = http.get(`${BASE_URL}/api/owners?page=0&size=5`, { tags: { endpoint: 'api_owners_list' } });
		check(list, { 'GET /api/owners -> 200': (r) => r.status === 200 });
	});
	hitLegado();
}

export default function loadTest() {
	if (MODE === 'modernizado') {
		hitModernizado();
	} else {
		hitLegado();
	}
	sleep(1);
}
