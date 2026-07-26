import http from 'k6/http';
import { check, group, sleep } from 'k6';

// Smoke test contra la instancia "modernizado" desplegada en AWS Academy (Fase 6).
// Cubre en paralelo la API REST de F-05 (/api/owners) y la UI Thymeleaf existente
// (/owners), para confirmar que ambas siguen respondiendo bajo carga mínima
// (patrón Wrapping: REST y MVC conviven sobre el mismo JAR).
// Laboratorio AWS Academy sin balanceador/TLS — todo se sirve en HTTP plano sobre
// el puerto 8080 por diseño (ver docs/experimento/ESTADO-PLAN.md, Fase 6).
const BASE_URL = __ENV.BASE_URL || 'http://18.233.169.133:8080'; // NOSONAR

export const options = {
	vus: 2,
	duration: '30s',
	thresholds: {
		http_req_failed: ['rate<0.01'],
		http_req_duration: ['p(95)<800'],
		'http_req_duration{endpoint:api_owner_by_id}': ['p(95)<500'],
		'http_req_duration{endpoint:api_owners_list}': ['p(95)<500'],
		'http_req_duration{endpoint:mvc_owner_by_id}': ['p(95)<800'],
		'http_req_duration{endpoint:mvc_owners_list}': ['p(95)<800'],
	},
};

export default function smokeTest() {
	group('API REST — /api/owners (F-05)', function () {
		const byId = http.get(`${BASE_URL}/api/owners/1`, {
			tags: { endpoint: 'api_owner_by_id' },
		});
		check(byId, {
			'GET /api/owners/1 -> 200': (r) => r.status === 200,
			'GET /api/owners/1 -> JSON con firstName': (r) => r.json('firstName') !== undefined,
		});

		const list = http.get(`${BASE_URL}/api/owners?page=0&size=5`, {
			tags: { endpoint: 'api_owners_list' },
		});
		check(list, {
			'GET /api/owners -> 200': (r) => r.status === 200,
			'GET /api/owners -> tiene content paginado': (r) => r.json('content') !== undefined,
		});

		const filtered = http.get(`${BASE_URL}/api/owners?lastName=Franklin`, {
			tags: { endpoint: 'api_owners_list' },
		});
		check(filtered, {
			'GET /api/owners?lastName=Franklin -> 200': (r) => r.status === 200,
		});

		// 404 esperado a propósito (OwnerNotFoundException -> ProblemDetail).
		// expectedStatuses evita que k6 lo cuente como http_req_failed.
		const notFound = http.get(`${BASE_URL}/api/owners/9999`, {
			tags: { endpoint: 'api_owner_not_found' },
			responseCallback: http.expectedStatuses(404),
		});
		check(notFound, {
			'GET /api/owners/9999 -> 404': (r) => r.status === 404,
		});
	});

	group('UI Thymeleaf — /owners (sin regresión)', function () {
		const ownerPage = http.get(`${BASE_URL}/owners/1`, {
			tags: { endpoint: 'mvc_owner_by_id' },
		});
		check(ownerPage, {
			'GET /owners/1 -> 200': (r) => r.status === 200,
			'GET /owners/1 -> HTML': (r) => r.headers['Content-Type'].includes('text/html'),
		});

		const ownersList = http.get(`${BASE_URL}/owners?lastName=`, {
			tags: { endpoint: 'mvc_owners_list' },
		});
		check(ownersList, {
			'GET /owners -> 200': (r) => r.status === 200,
		});
	});

	sleep(1);
}
