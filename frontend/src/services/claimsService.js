import { claimStatuses as mockClaimStatuses, mockClaims } from '../data/mockClaims'

const API_STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  PENDING: 'Pending',
  PAID: 'Paid',
  PARTIALLY_PAID: 'Partially Paid',
  DENIED: 'Denied',
  APPEALED: 'Appealed',
}
const API_CLAIM_STATUSES = Object.values(API_STATUS_LABELS)
const TOKEN_STORAGE_KEYS = ['accessToken', 'access_token', 'authToken', 'jwtToken', 'jwt', 'token']

const apiBaseUrl = (import.meta.env.VITE_CLAIMS_API_BASE_URL || '').trim().replace(/\/+$/, '')
export const isClaimsApiEnabled = Boolean(apiBaseUrl)
export const claimStatuses = isClaimsApiEnabled ? API_CLAIM_STATUSES : mockClaimStatuses

const cloneClaim = (claim) => ({
  ...claim,
  paymentHistory: claim.paymentHistory?.map((payment) => ({ ...payment })) ?? [],
  tasks: claim.tasks?.map((task) => ({ ...task })) ?? [],
  history: claim.history?.map((event) => ({ ...event })) ?? [],
  denial: claim.denial ? { ...claim.denial } : null,
})

const delay = () => new Promise((resolve) => setTimeout(resolve, 120))

function createMockClaimReference(claims) {
  const year = new Date().getFullYear()
  const firstSuffix = Number(String(Date.now()).slice(-5))
  const existingReferences = new Set(claims.map((claim) => String(claim.claimReference || claim.id || '').trim().toLowerCase()))

  for (let offset = 0; offset < 100000; offset += 1) {
    const suffix = String((firstSuffix + offset) % 100000).padStart(5, '0')
    const reference = `CLM-${year}-${suffix}`
    if (!existingReferences.has(reference.toLowerCase())) return reference
  }

  throw new Error(`Unable to generate a unique claim reference for ${year}.`)
}

function createMockAdapter() {
  let claims = mockClaims.map(cloneClaim)

  return {
    async getClaims() {
      await delay()
      return claims.map(cloneClaim)
    },

    async getClaimById(id) {
      await delay()
      const claim = claims.find((item) => item.id === id)
      return claim ? cloneClaim(claim) : null
    },

    async createClaim(values) {
      await delay()
      const claimReference = createMockClaimReference(claims)
      const claim = {
        ...values,
        claimReference,
        id: claimReference,
        initials: values.customer.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
        paidAmount: 0,
        status: 'Submitted',
        paymentHistory: [],
        denial: null,
        tasks: [],
        history: [{ title: 'Claim submitted', detail: 'Claim received for processing', date: new Date().toLocaleString(), done: true }],
      }
      claims = [claim, ...claims]
      return cloneClaim(claim)
    },

    async updateClaim(id, values) {
      await delay()
      const index = claims.findIndex((claim) => claim.id === id)
      if (index < 0) throw new Error(`Claim ${id} was not found`)
      claims[index] = { ...claims[index], ...values }
      return cloneClaim(claims[index])
    },

    async updateClaimStatus(id, status) {
      if (!mockClaimStatuses.includes(status)) throw new Error(`Unsupported claim status: ${status}`)
      await delay()
      const index = claims.findIndex((claim) => claim.id === id)
      if (index < 0) throw new Error(`Claim ${id} was not found`)
      const previous = claims[index].status
      claims[index] = {
        ...claims[index],
        status,
        history: [{
          title: `Status changed to ${status}`,
          detail: `Updated from ${previous}`,
          date: new Date().toLocaleString(),
          done: true,
        }, ...claims[index].history],
      }
      return cloneClaim(claims[index])
    },

  }
}

function getStoredToken() {
  for (const key of TOKEN_STORAGE_KEYS) {
    const stored = localStorage.getItem(key)
    if (!stored) continue

    let token = stored
    try {
      const parsed = JSON.parse(stored)
      if (typeof parsed === 'string') token = parsed
      else if (parsed && typeof parsed === 'object') {
        token = parsed.accessToken || parsed.access_token || parsed.token || ''
      }
    } catch {
      token = stored
    }

    if (typeof token === 'string' && token.trim()) {
      return token.trim().replace(/^Bearer\s+/i, '')
    }
  }
  return ''
}

function formatBackendError(payload, status) {
  const fallback = `Claims API request failed with status ${status}.`
  if (typeof payload === 'string') return payload.trim() || fallback
  if (!payload || typeof payload !== 'object') return fallback

  const directMessage = payload.detail || payload.message
    || (typeof payload.error === 'string' ? payload.error : payload.error?.message)
    || payload.title
  if (directMessage) return String(directMessage)

  if (Array.isArray(payload.errors)) {
    const messages = payload.errors.map((error) => {
      if (typeof error === 'string') return error
      return error.defaultMessage || error.message || error.field
    }).filter(Boolean)
    if (messages.length) return messages.join('; ')
  }

  if (payload.errors && typeof payload.errors === 'object') {
    const messages = Object.entries(payload.errors)
      .map(([field, message]) => `${field}: ${message}`)
    if (messages.length) return messages.join('; ')
  }

  return fallback
}

async function requestJson(path, options = {}) {
  const headers = { Accept: 'application/json' }
  const token = getStoredToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  let response
  try {
    response = await fetch(`${apiBaseUrl}/api/v1/claims${path}`, {
      method: options.method || 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(`Could not reach the Claims API at ${apiBaseUrl}. Check that Spring Boot is running and CORS allows this frontend origin.`)
    }
    throw error
  }

  if (!response.ok) {
    const responseText = await response.text()
    let payload = responseText
    if (responseText) {
      try {
        payload = JSON.parse(responseText)
      } catch {
        payload = responseText
      }
    }
    throw new Error(formatBackendError(payload, response.status))
  }
  if (response.status === 204) return null

  const responseText = await response.text()
  if (!responseText) return null
  try {
    return JSON.parse(responseText)
  } catch {
    throw new Error('Claims API returned an invalid JSON response.')
  }
}

function normalizeStatus(status) {
  const key = String(status || '').trim().toUpperCase().replace(/[\s-]+/g, '_')
  if (API_STATUS_LABELS[key]) return API_STATUS_LABELS[key]
  if (key === 'UNDER_REVIEW') return 'Pending'
  return String(status || 'Unknown').replaceAll('_', ' ')
}

function toApiStatus(status) {
  const key = String(status || '').trim().toUpperCase().replace(/[\s-]+/g, '_')
  if (key === 'UNDER_REVIEW') return 'PENDING'
  if (API_STATUS_LABELS[key]) return key
  throw new Error(`The Claims API does not support the "${status}" status.`)
}

function normalizeOptionalRecordList(claim, field, claimReference) {
  if (!Object.prototype.hasOwnProperty.call(claim, field)) {
    return { available: false, records: [] }
  }

  const records = claim[field]
  if (records == null) return { available: true, records: [] }
  if (!Array.isArray(records)) {
    throw new Error(`Claims API claim ${claimReference} has an invalid "${field}" value; expected an array.`)
  }
  return { available: true, records }
}

function normalizeOptionalDenial(claim, claimReference) {
  if (!Object.prototype.hasOwnProperty.call(claim, 'denial')) {
    return { available: false, incident: null }
  }

  const incident = claim.denial
  if (incident == null) return { available: true, incident: null }
  if (typeof incident !== 'object' || Array.isArray(incident)) {
    throw new Error(`Claims API claim ${claimReference} has an invalid "denial" value; expected an object or null.`)
  }
  return { available: true, incident }
}

function normalizeApiClaim(claim) {
  if (!claim || typeof claim !== 'object' || claim.claimId == null) {
    throw new Error('Claims API returned a claim without the required claimId.')
  }
  if (claim.claimReference == null || !String(claim.claimReference).trim()) {
    throw new Error(`Claims API claim ${claim.claimId} is missing claimReference.`)
  }

  const patientRef = claim.patientRef || ''
  const amount = Number(claim.claimAmount)
  if (!Number.isFinite(amount)) {
    throw new Error(`Claims API claim ${claim.claimReference} has an invalid claimAmount.`)
  }

  const paymentHistory = normalizeOptionalRecordList(claim, 'paymentHistory', claim.claimReference)
  const tasks = normalizeOptionalRecordList(claim, 'tasks', claim.claimReference)
  const denial = normalizeOptionalDenial(claim, claim.claimReference)

  return {
    apiMode: true,
    apiId: claim.claimId,
    id: String(claim.claimReference),
    customer: '',
    initials: '',
    memberId: patientRef,
    insurer: '',
    claimType: '',
    submittedDate: claim.submissionDate || '',
    claimedAmount: amount,
    approvedAmount: null,
    paidAmount: null,
    status: normalizeStatus(claim.currentStatus),
    officer: '',
    provider: '',
    serviceDate: '',
    diagnosis: '',
    policyNumber: '',
    email: '',
    phone: '',
    paymentHistory: paymentHistory.records,
    denial: denial.incident,
    tasks: tasks.records,
    relatedDataAvailability: {
      payments: paymentHistory.available,
      denial: denial.available,
      tasks: tasks.available,
    },
    history: [],
  }
}

function normalizeClaimHistory(events) {
  if (!Array.isArray(events)) {
    throw new Error('Claims API history endpoint must return an array.')
  }
  return events
    .map((event) => {
      const previousStatus = event.previousStatus ? normalizeStatus(event.previousStatus) : ''
      const newStatus = normalizeStatus(event.newStatus)
      const changedAt = event.changedAt ? new Date(event.changedAt) : null
      const date = changedAt && !Number.isNaN(changedAt.getTime())
        ? changedAt.toLocaleString()
        : String(event.changedAt || 'Date unavailable')
      return {
        title: `Status changed to ${newStatus}`,
        detail: previousStatus ? `Updated from ${previousStatus}` : 'Initial claim status',
        date,
        changedAt: changedAt?.getTime() || 0,
      }
    })
    .sort((left, right) => right.changedAt - left.changedAt)
    .map(({ changedAt, ...event }) => event)
}

function requiredRequestValue(value, label) {
  const normalized = String(value ?? '').trim()
  if (!normalized) throw new Error(`${label} is required by the Claims API.`)
  return normalized
}

function createHttpAdapter() {
  const claimsByReference = new Map()

  const remember = (claim) => {
    claimsByReference.set(claim.id, claim)
    claimsByReference.set(String(claim.apiId), claim)
    return claim
  }

  const getApiId = (id) => {
    const claim = claimsByReference.get(String(id))
    if (claim) return claim.apiId
    if (/^\d+$/.test(String(id))) return String(id)
    throw new Error(`Claim ${id} has no API claimId. Reload the claims list and try again.`)
  }

  return {
    async getClaims() {
      const firstPage = await requestJson('?page=0&size=100')
      if (Array.isArray(firstPage)) return firstPage.map((claim) => remember(normalizeApiClaim(claim)))
      if (!firstPage || !Array.isArray(firstPage.content)) {
        throw new Error('Claims API list response must contain a paginated "content" array.')
      }

      const claims = firstPage.content.map((claim) => remember(normalizeApiClaim(claim)))
      const totalPages = Number(firstPage.totalPages) || 1
      for (let page = 1; page < totalPages; page += 1) {
        const pageResult = await requestJson(`?page=${page}&size=100`)
        if (!pageResult || !Array.isArray(pageResult.content)) {
          throw new Error(`Claims API page ${page} did not contain a "content" array.`)
        }
        claims.push(...pageResult.content.map((claim) => remember(normalizeApiClaim(claim))))
      }
      return claims
    },

    async getClaimById(id) {
      const apiId = getApiId(id)
      const claim = normalizeApiClaim(await requestJson(`/${encodeURIComponent(apiId)}`))
      const history = await requestJson(`/${encodeURIComponent(apiId)}/history`)
      claim.history = normalizeClaimHistory(history)
      return remember(claim)
    },

    async createClaim(values) {
      const body = {
        claimReference: requiredRequestValue(values.claimReference, 'Claim reference'),
        patientRef: requiredRequestValue(values.memberId, 'Patient reference'),
        claimAmount: Number(values.claimedAmount),
        submissionDate: requiredRequestValue(values.submittedDate, 'Submission date'),
      }
      const created = normalizeApiClaim(await requestJson('', { method: 'POST', body }))
      return remember(created)
    },

    async updateClaim(id, values) {
      const apiId = getApiId(id)
      const previous = claimsByReference.get(String(id))
      const body = {
        patientRef: requiredRequestValue(values.memberId, 'Patient reference'),
        claimAmount: Number(values.claimedAmount),
        submissionDate: requiredRequestValue(values.submittedDate, 'Submission date'),
      }
      const updated = normalizeApiClaim(await requestJson(`/${encodeURIComponent(apiId)}`, { method: 'PUT', body }))
      updated.history = previous?.history || []
      return remember(updated)
    },

    async updateClaimStatus(id, status) {
      const apiId = getApiId(id)
      const previous = claimsByReference.get(String(id))
      const updated = normalizeApiClaim(await requestJson(`/${encodeURIComponent(apiId)}/status`, {
        method: 'PATCH',
        body: { newStatus: toApiStatus(status) },
      }))
      updated.history = previous?.history || []
      return remember(updated)
    },

  }
}

const adapter = isClaimsApiEnabled ? createHttpAdapter() : createMockAdapter()

export const claimsService = {
  getClaims: () => adapter.getClaims(),
  getClaimById: (id) => adapter.getClaimById(id),
  createClaim: (values) => adapter.createClaim(values),
  updateClaim: (id, values) => adapter.updateClaim(id, values),
  updateClaimStatus: (id, status) => adapter.updateClaimStatus(id, status),
}
