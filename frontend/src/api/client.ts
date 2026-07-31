const API_BASE_URL = (
    import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'
).replace(/\/$/, '')

const ACCESS_TOKEN_KEY = "unitrovee_access_token"

export type ApiRequestOptions = RequestInit & {
    redirectOnUnauthorized?: boolean
}

export class ApiError extends Error {
    public readonly status: number
    public readonly body: unknown

    constructor(status: number, body: unknown) {
        super(`API request failed with status ${status}`);
        this.name = 'ApiError'
        this.status = status
        this.body = body
    }
}

export function getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export async function apiClient<T>(
    path: string,
    options: ApiRequestOptions = {},
): Promise<T> {
    const {
        headers,
        redirectOnUnauthorized = true,
        ...requestOptions
    } = options

    const requestHeaders = new Headers(headers)
    requestHeaders.set('Accept', 'application/json')

    const token = getAccessToken()

    if (token) {
        requestHeaders.set('Authorization', `Bearer ${token}`)
    }

    const response = await fetch(toApiUrl(path), {
        ...requestOptions,
        headers: requestHeaders,
    })

    const body = await parseResponseBody(response)

    if (response.status === 401 && redirectOnUnauthorized) {
        clearAccessToken()

        // login calls will use false so invalid credentials stay on the login page
        if (window.location.pathname !== '/login') {
            window.location.assign('/login')
        }
    }

    if (!response.ok) {
        throw new ApiError(response.status, body)
    }

    return body as T
}

function toApiUrl(path: string) {
    return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

async function parseResponseBody(response: Response): Promise<unknown> {
    if (response.status === 204) {
        return undefined
    }

    const contentType = response.headers.get('content-type') ?? ''

    if (contentType.includes('json')) {
        return response.json()
    }

    return response.text()
}