import { useEffect, useMemo, useState } from 'react'
import {
  Activity, ArrowDown, ArrowDownUp, ArrowLeft, ArrowUp, Bell, CalendarDays,
  Check, ChevronDown, ChevronLeft, ChevronRight, CircleHelp, ClipboardList,
  Clock3, Download, FileCheck2, FilePlus2, FileText, Filter, LayoutDashboard,
  ListChecks, LoaderCircle, Menu, Moon, MoreHorizontal, Search, Shield, Sun,
  UserRound, Users, WalletCards, X,
} from 'lucide-react'
import { claimStatuses, claimsService, isClaimsApiEnabled } from './services/claimsService'

const navigation = [
  { section: 'WORKSPACE', items: [
    { label: 'Dashboard', icon: LayoutDashboard },
    { label: 'Claims Management', icon: ClipboardList, route: 'claims' },
    { label: 'Denial Management', icon: FileCheck2 },
    { label: 'Follow-up Tasks', icon: ListChecks },
    { label: 'Payment Tracking', icon: WalletCards },
  ] },
  { section: 'INSIGHTS & ADMIN', items: [
    { label: 'Audit Trail', icon: Activity },
    { label: 'Reports & Analytics', icon: FileText },
    { label: 'User & Role Management', icon: Users },
    { label: 'AI Chatbot', icon: CircleHelp },
  ] },
]

const emptyClaim = {
  claimReference: '',
  customer: '',
  insurer: '',
  claimType: 'Outpatient',
  submittedDate: new Date().toISOString().slice(0, 10),
  claimedAmount: '',
  paidAmount: 0,
  officer: '',
  provider: '',
  serviceDate: '',
  diagnosis: '',
  policyNumber: '',
  memberId: '',
  email: '',
  phone: '',
}

const money = (amount) => {
  const value = Number(amount || 0)
  return `Rs. ${value.toLocaleString('en-LK', { minimumFractionDigits: Number.isInteger(value) ? 0 : 2, maximumFractionDigits: 2 })}`
}
const displayDate = (date) => date ? new Date(`${date}T00:00:00`).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
const statusClass = (status) => `badge badge-${status.toLowerCase().replace(/\s+/g, '-')}`
const compactOfficerName = (name) => {
  const parts = name.trim().split(/\s+/)
  return parts.length > 1 ? `${parts[0]} ${parts[parts.length - 1][0]}.` : parts[0]
}

function App() {
  const [claims, setClaims] = useState([])
  const [claimsLoading, setClaimsLoading] = useState(true)
  const [claimsError, setClaimsError] = useState('')
  const [reloadClaims, setReloadClaims] = useState(0)
  const [selectedId, setSelectedId] = useState(null)
  const [detailClaim, setDetailClaim] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [reloadDetails, setReloadDetails] = useState(0)
  const [activeNav, setActiveNav] = useState('Claims Management')
  const [theme, setTheme] = useState(() => localStorage.getItem('claims-theme') || 'dark')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('All statuses')
  const [insurerFilter, setInsurerFilter] = useState('All insurers')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [sort, setSort] = useState({ key: 'submittedDate', direction: 'desc' })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(8)
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(emptyClaim)
  const [formError, setFormError] = useState('')
  const [nextStatus, setNextStatus] = useState('')
  const [notice, setNotice] = useState('')
  const [mobileNav, setMobileNav] = useState(false)
  const [savingClaim, setSavingClaim] = useState(false)
  const [savingStatus, setSavingStatus] = useState(false)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('claims-theme', theme)
  }, [theme])

  useEffect(() => {
    let active = true
    setClaimsLoading(true)
    setClaimsError('')
    claimsService.getClaims()
      .then((items) => {
        if (active) setClaims(items)
      })
      .catch((error) => {
        if (active) setClaimsError(error.message || 'Unable to load claims.')
      })
      .finally(() => {
        if (active) setClaimsLoading(false)
      })
    return () => { active = false }
  }, [reloadClaims])

  useEffect(() => {
    if (!selectedId) {
      setDetailClaim(null)
      setDetailError('')
      setDetailLoading(false)
      return undefined
    }

    let active = true
    setDetailClaim(null)
    setDetailError('')
    setDetailLoading(true)
    claimsService.getClaimById(selectedId)
      .then((claim) => {
        if (!active) return
        if (claim) setDetailClaim(claim)
        else setDetailError(`Claim ${selectedId} was not found.`)
      })
      .catch((error) => {
        if (active) setDetailError(error.message || 'Unable to load claim details.')
      })
      .finally(() => {
        if (active) setDetailLoading(false)
      })
    return () => { active = false }
  }, [selectedId, reloadDetails])

  const selectedClaim = detailClaim?.id === selectedId ? detailClaim : null
  const insurers = [...new Set(claims.map((claim) => String(claim.insurer ?? '').trim()).filter(Boolean))].sort()
  const filteredClaims = useMemo(() => {
    const needle = search.trim().toLowerCase()
    const list = claims.filter((claim) => {
      const matchesSearch = !needle || claim.id.toLowerCase().includes(needle) || String(claim.customer || claim.memberId || '').toLowerCase().includes(needle)
      const matchesStatus = statusFilter === 'All statuses' || claim.status === statusFilter
      const matchesInsurer = insurerFilter === 'All insurers' || String(claim.insurer ?? '').trim() === insurerFilter.trim()
      const matchesFrom = !dateFrom || claim.submittedDate >= dateFrom
      const matchesTo = !dateTo || claim.submittedDate <= dateTo
      return matchesSearch && matchesStatus && matchesInsurer && matchesFrom && matchesTo
    })
    return list.sort((a, b) => {
      const left = a[sort.key]
      const right = b[sort.key]
      const comparison = typeof left === 'number' ? left - right : String(left ?? '').localeCompare(String(right ?? ''))
      return sort.direction === 'asc' ? comparison : -comparison
    })
  }, [claims, search, statusFilter, insurerFilter, dateFrom, dateTo, sort])
  const hasActiveFilters = Boolean(search.trim() || statusFilter !== 'All statuses' || insurerFilter !== 'All insurers' || dateFrom || dateTo)

  const pageCount = Math.max(1, Math.ceil(filteredClaims.length / pageSize))
  const visibleClaims = filteredClaims.slice((page - 1) * pageSize, page * pageSize)
  const summary = {
    total: claims.length,
    pending: claims.filter((claim) => ['Submitted', 'Under Review', 'Pending'].includes(claim.status)).length,
    approved: claims.filter((claim) => ['Approved', 'Paid', 'Partially Paid'].includes(claim.status)).length,
    denied: claims.filter((claim) => claim.status === 'Denied').length,
  }

  const updateClaim = (updated) => {
    setClaims((current) => current.map((claim) => claim.id === updated.id ? updated : claim))
    setDetailClaim((current) => current?.id === updated.id ? updated : current)
  }
  const clearFilters = () => {
    setSearch('')
    setStatusFilter('All statuses')
    setInsurerFilter('All insurers')
    setDateFrom('')
    setDateTo('')
    setPage(1)
  }
  const openNewClaim = () => {
    setForm({ ...emptyClaim, submittedDate: new Date().toISOString().slice(0, 10) })
    setFormError('')
    setModal({ type: 'claim' })
  }
  const openEditClaim = () => {
    if (!selectedClaim) return
    setForm({
      ...selectedClaim,
      claimReference: selectedClaim.id,
      memberId: selectedClaim.memberId || '',
      claimedAmount: String(selectedClaim.claimedAmount),
      paidAmount: String(selectedClaim.paidAmount ?? ''),
    })
    setFormError('')
    setModal({ type: 'claim', editId: selectedClaim.id })
  }

  const saveClaim = async (event) => {
    event.preventDefault()
    setFormError('')
    if (!modal.editId && isClaimsApiEnabled) {
      const claimReference = String(form.claimReference ?? '').trim().toLowerCase()
      const duplicateReference = claims.some((claim) => String(claim.id || claim.claimReference || '').trim().toLowerCase() === claimReference)
      if (duplicateReference) {
        setFormError('A claim with this reference already exists.')
        return
      }
    }
    const amount = Number(form.claimedAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setFormError('Enter a claimed amount greater than zero.')
      return
    }
    if (form.paidAmount !== '' && (!Number.isFinite(Number(form.paidAmount)) || Number(form.paidAmount) < 0)) {
      setFormError('Paid amount must be zero or more.')
      return
    }
    const values = { ...form, claimedAmount: amount, paidAmount: Number(form.paidAmount || 0) }
    setSavingClaim(true)
    try {
      if (modal.editId) {
        const updated = await claimsService.updateClaim(modal.editId, values)
        updateClaim(updated)
        setNotice('Claim details updated.')
      } else {
        const created = await claimsService.createClaim(values)
        setClaims((current) => [created, ...current])
        setPage(1)
        setNotice('New claim added.')
      }
      setModal(null)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setSavingClaim(false)
    }
  }

  const saveStatus = async (event) => {
    event.preventDefault()
    if (!selectedClaim || !nextStatus) return
    setSavingStatus(true)
    try {
      const updated = await claimsService.updateClaimStatus(selectedClaim.id, nextStatus)
      updateClaim(updated)
      setModal(null)
      setNotice(`Claim status updated to ${nextStatus}.`)
      if (isClaimsApiEnabled) setReloadDetails((current) => current + 1)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setSavingStatus(false)
    }
  }

  const exportCsv = () => {
    const columns = [
      ['CLAIM ID', 'id'], ['CUSTOMER', 'customer'], ['INSURER', 'insurer'], ['CLAIM TYPE', 'claimType'],
      ['SUBMITTED DATE', 'submittedDate'], ['CLAIMED AMOUNT', 'claimedAmount'], ['PAID AMOUNT', 'paidAmount'],
      ['CLAIM STATUS', 'status'], ['ASSIGNED OFFICER', 'officer'],
    ]
    const csv = [
      columns.map(([label]) => label).join(','),
      ...filteredClaims.map((claim) => columns.map(([, key]) => `"${String(claim[key] ?? '').replaceAll('"', '""')}"`).join(',')),
    ].join('\r\n')
    const link = document.createElement('a')
    link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    link.download = 'claims-export.csv'
    link.click()
    URL.revokeObjectURL(link.href)
  }

  const changeSort = (key) => {
    setSort((current) => current.key === key
      ? { key, direction: current.direction === 'asc' ? 'desc' : 'asc' }
      : { key, direction: 'asc' })
  }
  const setNavigation = (item) => {
    setActiveNav(item.label)
    setSelectedId(null)
    setMobileNav(false)
  }

  return (
    <div className="app-shell">
      {mobileNav && <button className="scrim" aria-label="Close navigation" onClick={() => setMobileNav(false)} />}
      <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}>
        <div className="brand">
          <div className="brand-logo"><Activity size={21} strokeWidth={2.2} /></div>
          <div><strong>innobot<span>health</span></strong><small>ADMIN CONSOLE / RCM</small></div>
          <button className="icon-btn mobile-close" onClick={() => setMobileNav(false)} aria-label="Close navigation"><X size={18} /></button>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        {navigation.map((group) => (
          <div className="nav-group" key={group.section}>
            {group.section !== 'WORKSPACE' && <p className="nav-section">{group.section}</p>}
            {group.items.map(({ label, icon: Icon, route }) => (
              <button
                className={`nav-link ${activeNav === label && !selectedId ? 'nav-active' : ''}`}
                key={label}
                onClick={() => route === 'claims' ? setNavigation({ label }) : setNavigation({ label })}
              >
                <Icon size={18} strokeWidth={1.8} /><span>{label}</span>
                {label === 'Claims Management' && <span className="nav-count">{claims.length}</span>}
              </button>
            ))}
          </div>
        ))}
        <div className="sidebar-bottom">
          <div className="sidebar-user">
            <div className="avatar avatar-user">NF</div>
            <div className="user-copy"><strong>Nimal Fernando</strong><small>Claims Processing Officer</small></div>
            <MoreHorizontal size={17} className="user-more" />
          </div>
        </div>
      </aside>

      <main className="main-panel">
        <header className="top-header">
          <button className="icon-btn nav-toggle" onClick={() => setMobileNav(true)} aria-label="Open navigation"><Menu size={19} /></button>
          <div className="breadcrumb"><span>Workspace</span><ChevronRight size={13} /><span>{selectedId ? `Claims Management / ${selectedId}` : activeNav}</span></div>
          <label className="global-search"><Search size={16} /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(1) }} placeholder="Search claims, customers, tasks..." /></label>
          <div className="header-actions">
            <button className="theme-pill" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
              {theme === 'dark' ? <Moon size={14} /> : <Sun size={14} />}
              {theme.toUpperCase()}
            </button>
            <button className="icon-btn notification-btn" aria-label="Notifications"><Bell size={18} /><i /></button>
            <div className="header-avatar">NF</div>
          </div>
        </header>

        <div className="page-content">
          {notice && <div className="notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="Dismiss notification"><X size={15} /></button></div>}
          {claimsError && activeNav === 'Claims Management' && !selectedId && (
            <div className="error-banner" role="alert">
              <span><strong>Claims could not be loaded.</strong> {claimsError}</span>
              <button onClick={() => setReloadClaims((current) => current + 1)}>Try again</button>
            </div>
          )}
          {activeNav !== 'Claims Management' ? (
            <PlaceholderPage title={activeNav} />
          ) : selectedId && detailLoading ? (
            <PageState loading title="Loading claim details" description={`Retrieving ${selectedId}...`} />
          ) : selectedId && detailError ? (
            <PageState title="Unable to load claim" description={detailError} actionLabel="Try again" onAction={() => setReloadDetails((current) => current + 1)} secondaryActionLabel="Back to claims" onSecondaryAction={() => setSelectedId(null)} />
          ) : selectedId && selectedClaim ? (
            <ClaimDetails
              claim={selectedClaim}
              onBack={() => setSelectedId(null)}
              onEdit={openEditClaim}
              onStatus={() => { setNextStatus(selectedClaim.status); setFormError(''); setModal({ type: 'status' }) }}
            />
          ) : selectedId ? (
            <PageState loading title="Loading claim details" description={`Retrieving ${selectedId}...`} />
          ) : (
            <>
              <div className="page-heading">
                <div>
                  <div className="overline">REVENUE CYCLE MANAGEMENT <span>•</span> OPERATIONS</div>
                  <h1>Claims Management</h1>
                  <p className="subheading">Review, process, and track all insurance claims.</p>
                </div>
                <div className="page-heading-actions">
                  <button className="button button-outline" onClick={exportCsv} disabled={claimsLoading || Boolean(claimsError) || filteredClaims.length === 0}><Download size={16} /> Export</button>
                  <button className="button button-primary" onClick={openNewClaim}>+ Add New Claim</button>
                </div>
              </div>

              <section className="kpi-grid" aria-label="Claims summary">
                <KpiCard label="Total Claims" value={summary.total} icon={ClipboardList} tone="blue" detail="vs. last month" trend="+8.2%" loading={claimsLoading || Boolean(claimsError)} />
                <KpiCard label="Pending" value={summary.pending} icon={Clock3} tone="amber" detail="vs. last month" trend="-3.1%" loading={claimsLoading || Boolean(claimsError)} />
                <KpiCard label={isClaimsApiEnabled ? 'Paid / Partially Paid' : 'Approved'} value={summary.approved} icon={Check} tone="green" detail="vs. last month" trend="+12.4%" loading={claimsLoading || Boolean(claimsError)} />
                <KpiCard label="Denied" value={summary.denied} icon={Shield} tone="red" detail="vs. last month" trend="-2.6%" loading={claimsLoading || Boolean(claimsError)} />
              </section>

              <section className="panel claims-panel" aria-busy={claimsLoading}>
                <div className="panel-heading">
                  <div><h2>All Claims</h2><p>Manage and review submitted claims</p></div>
                </div>
                <div className="filters">
                  <label className="search-field"><Search size={16} /><input value={search} onChange={(event) => { setSearch(event.target.value); setPage(1) }} placeholder="Search claim ID or customer..." /></label>
                  <label className="filter-field"><Filter size={15} /><select value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(1) }}><option>All statuses</option>{claimStatuses.map((status) => <option key={status}>{status}</option>)}</select><ChevronDown size={14} /></label>
                  <label className="filter-field insurer-field"><Shield size={15} /><select value={insurerFilter} onChange={(event) => { setInsurerFilter(event.target.value); setPage(1) }}><option>All insurers</option>{insurers.map((insurer) => <option key={insurer}>{insurer}</option>)}</select><ChevronDown size={14} /></label>
                  <label className="date-filter"><CalendarDays size={15} /><input aria-label="Submitted from" type="date" value={dateFrom} onChange={(event) => { setDateFrom(event.target.value); setPage(1) }} /><span>to</span><input aria-label="Submitted to" type="date" value={dateTo} onChange={(event) => { setDateTo(event.target.value); setPage(1) }} /></label>
                </div>
                <div className="table-scroll">
                  <table className="claims-table">
                    <thead><tr>
                      <SortHeader label="CLAIM ID" field="id" sort={sort} onSort={changeSort} />
                      <SortHeader label="CUSTOMER" field="customer" sort={sort} onSort={changeSort} />
                      <SortHeader label="INSURER" field="insurer" sort={sort} onSort={changeSort} />
                      <th>CLAIM TYPE</th>
                      <SortHeader label="SUBMITTED DATE" field="submittedDate" sort={sort} onSort={changeSort} />
                      <SortHeader label="CLAIMED AMOUNT" field="claimedAmount" sort={sort} onSort={changeSort} align="right" />
                      <th className="align-right">PAID AMOUNT</th>
                      <th>CLAIM STATUS</th>
                      <th>ASSIGNED OFFICER</th>
                    </tr></thead>
                    <tbody>
                      {claimsLoading ? (
                        <tr><td colSpan="9"><TableState loading label="Loading claims..." /></td></tr>
                      ) : claimsError ? (
                        <tr><td colSpan="9"><TableState label="The claims list is unavailable. Use “Try again” above to reload it." /></td></tr>
                      ) : visibleClaims.length === 0 ? (
                        <tr><td colSpan="9"><TableState
                          label={claims.length === 0 ? 'No claims yet. Add a claim to get started.' : 'No claims match the current filters.'}
                          actionLabel={claims.length === 0 ? 'Add New Claim' : hasActiveFilters ? 'Clear filters' : undefined}
                          onAction={claims.length === 0 ? openNewClaim : clearFilters}
                        /></td></tr>
                      ) : visibleClaims.map((claim) => (
                        <tr key={claim.id} onClick={() => setSelectedId(claim.id)} className="clickable-row">
                          <td><button className="claim-reference" onClick={(event) => { event.stopPropagation(); setSelectedId(claim.id) }}>{claim.id}</button></td>
                          <td><div className="customer-cell">{claim.customer && <div className="avatar avatar-small">{claim.initials}</div>}<span>{claim.customer || (claim.memberId ? `Patient ref ${claim.memberId}` : '—')}</span></div></td>
                          <td>{claim.insurer}</td>
                          <td>{claim.claimType}</td>
                          <td>{displayDate(claim.submittedDate)}</td>
                          <td className="money-cell">{money(claim.claimedAmount)}</td>
                          <td className="money-cell">{claim.apiMode ? '—' : money(claim.paidAmount)}</td>
                          <td><span className={statusClass(claim.status)}><i />{claim.status}</span></td>
                          <td>{claim.officer ? <span className="officer" aria-label={`Assigned officer: ${claim.officer}`}><i aria-hidden="true">{claim.officer.split(/\s+/).map((part) => part[0]).join('')}</i><span title={claim.officer}>{compactOfficerName(claim.officer)}</span></span> : '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="table-footer">
                  <span>Showing <strong>{filteredClaims.length ? (page - 1) * pageSize + 1 : 0}–{Math.min(page * pageSize, filteredClaims.length)}</strong> of <strong>{filteredClaims.length}</strong> claims</span>
                  <div className="pagination">
                    <label>Rows per page <select value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(1) }}><option value="5">5</option><option value="8">8</option><option value="15">15</option></select></label>
                    <button className="page-arrow" aria-label="Previous page" disabled={page <= 1} onClick={() => setPage((current) => current - 1)}><ChevronLeft size={16} /></button>
                    <span className="page-indicator">{page} / {pageCount}</span>
                    <button className="page-arrow" aria-label="Next page" disabled={page >= pageCount} onClick={() => setPage((current) => current + 1)}><ChevronRight size={16} /></button>
                  </div>
                </div>
              </section>
              <p className="page-footnote">CLAIMS DATA REFRESHED JUST NOW <span>•</span> {isClaimsApiEnabled ? 'LIVE API' : 'MOCK DATA'} ENVIRONMENT</p>
            </>
          )}
        </div>
      </main>

      {modal?.type === 'claim' && (
        <Modal
          title={modal.editId ? 'Edit Claim' : 'Add New Claim'}
          description={isClaimsApiEnabled ? 'Enter the fields supported by the Claims API.' : 'Enter the claim and customer details below.'}
          onClose={() => setModal(null)}
        >
          <form onSubmit={saveClaim} className="claim-form">
            <div className="form-grid">
              {isClaimsApiEnabled ? (
                <>
                  {!modal.editId && <Field label="Claim reference" required><input autoFocus required maxLength="30" value={form.claimReference} onChange={(event) => setForm({ ...form, claimReference: event.target.value })} placeholder="CLM-2026-00193" /></Field>}
                  <Field label="Patient reference" required><input required maxLength="50" value={form.memberId || ''} onChange={(event) => setForm({ ...form, memberId: event.target.value })} placeholder="Patient reference" /></Field>
                  <Field label="Submission date" required><input required type="date" value={form.submittedDate} onChange={(event) => setForm({ ...form, submittedDate: event.target.value })} /></Field>
                  <Field label="Claimed amount (Rs.)" required><input required type="number" min="0.01" step="0.01" value={form.claimedAmount} onChange={(event) => setForm({ ...form, claimedAmount: event.target.value })} placeholder="0.00" /></Field>
                </>
              ) : (
                <>
                  <Field label="Customer name" required><input autoFocus required value={form.customer} onChange={(event) => setForm({ ...form, customer: event.target.value })} placeholder="Full name" /></Field>
                  <Field label="Insurer" required><input required value={form.insurer} onChange={(event) => setForm({ ...form, insurer: event.target.value })} placeholder="Insurance provider" /></Field>
                  <Field label="Claim type" required><select required value={form.claimType} onChange={(event) => setForm({ ...form, claimType: event.target.value })}>{['Inpatient', 'Outpatient', 'Emergency', 'Specialist', 'Diagnostic', 'Therapy'].map((type) => <option key={type}>{type}</option>)}</select></Field>
                  <Field label="Submitted date" required><input required type="date" value={form.submittedDate} onChange={(event) => setForm({ ...form, submittedDate: event.target.value })} /></Field>
                  <Field label="Claimed amount (Rs.)" required><input required type="number" min="0.01" step="0.01" value={form.claimedAmount} onChange={(event) => setForm({ ...form, claimedAmount: event.target.value })} placeholder="0.00" /></Field>
                  <Field label="Assigned officer" required><input required value={form.officer} onChange={(event) => setForm({ ...form, officer: event.target.value })} placeholder="Officer name" /></Field>
                  <Field label="Provider"><input value={form.provider || ''} onChange={(event) => setForm({ ...form, provider: event.target.value })} placeholder="Healthcare provider" /></Field>
                  <Field label="Policy number"><input value={form.policyNumber || ''} onChange={(event) => setForm({ ...form, policyNumber: event.target.value })} placeholder="Policy number" /></Field>
                </>
              )}
            </div>
            {formError && <p className="form-error" role="alert">{formError}</p>}
            <div className="modal-actions"><button type="button" className="button button-outline" onClick={() => setModal(null)} disabled={savingClaim}>Cancel</button><button type="submit" className="button button-primary" disabled={savingClaim}>{savingClaim ? 'Saving…' : modal.editId ? 'Save Changes' : 'Add Claim'}</button></div>
          </form>
        </Modal>
      )}
      {modal?.type === 'status' && selectedClaim && (
        <Modal title="Update Claim Status" description={`Choose a new status for ${selectedClaim.id}.`} onClose={() => setModal(null)}>
          <form onSubmit={saveStatus} className="status-form">
            <Field label="Claim status" required><select value={nextStatus} onChange={(event) => setNextStatus(event.target.value)}>{claimStatuses.map((status) => <option key={status}>{status}</option>)}</select></Field>
            {formError && <p className="form-error" role="alert">{formError}</p>}
            <div className="modal-actions"><button type="button" className="button button-outline" onClick={() => setModal(null)} disabled={savingStatus}>Cancel</button><button type="submit" className="button button-primary" disabled={savingStatus}>{savingStatus ? 'Updating…' : 'Update Status'}</button></div>
          </form>
        </Modal>
      )}
    </div>
  )
}

function SortHeader({ label, field, sort, onSort, align }) {
  const Icon = sort.key !== field ? ArrowDownUp : sort.direction === 'asc' ? ArrowUp : ArrowDown
  return <th className={align === 'right' ? 'align-right' : ''}><button className="sort-button" onClick={() => onSort(field)}>{label}<Icon size={12} /></button></th>
}

function KpiCard({ label, value, icon: Icon, tone, detail, trend, loading }) {
  return <article className="kpi-card" aria-busy={loading}><div className="kpi-top"><div className={`kpi-icon kpi-${tone}`}><Icon size={17} /></div><div className="kpi-label">{label}</div></div><div className="kpi-value">{loading ? '—' : value.toLocaleString()}</div><div className="kpi-meta"><span>{detail}</span>{!loading && <span className={`kpi-trend trend-${tone}`}>{trend}</span>}</div><div className={`kpi-accent accent-${tone}`} /></article>
}

function TableState({ loading = false, label, actionLabel, onAction }) {
  return <div className="table-state" role="status" aria-live="polite">
    {loading && <LoaderCircle size={18} className="state-spinner" />}
    <span>{label}</span>
    {actionLabel && <button className="state-action" onClick={onAction}>{actionLabel}</button>}
  </div>
}

function PageState({ loading = false, title, description, actionLabel, onAction, secondaryActionLabel, onSecondaryAction }) {
  return <section className={`page-state ${loading ? 'page-state-loading' : ''}`} role={loading ? 'status' : 'alert'} aria-live="polite">
    {loading ? <LoaderCircle size={22} className="state-spinner" /> : <CircleHelp size={22} />}
    <h1>{title}</h1>
    <p>{description}</p>
    {(actionLabel || secondaryActionLabel) && <div className="page-state-actions">
      {actionLabel && <button className="button button-primary" onClick={onAction}>{actionLabel}</button>}
      {secondaryActionLabel && <button className="button button-outline" onClick={onSecondaryAction}>{secondaryActionLabel}</button>}
    </div>}
  </section>
}

function Field({ label, required, children }) {
  return <label className="form-field"><span>{label}{required && <b> *</b>}</span>{children}</label>
}

function Modal({ title, description, onClose, children }) {
  useEffect(() => {
    const closeOnEscape = (event) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className="modal-heading"><div><h2 id="modal-title">{title}</h2><p>{description}</p></div><button className="icon-btn" onClick={onClose} aria-label="Close dialog"><X size={18} /></button></div>
      {children}
    </section>
  </div>
}

function InfoCard({ title, icon: Icon, children, className = '' }) {
  return <section className={`panel detail-card ${className}`}><div className="detail-card-title"><span><Icon size={16} /></span><h2>{title}</h2></div>{children}</section>
}

function DataPair({ label, value }) {
  return <div className="data-pair"><span>{label}</span><strong>{value || '—'}</strong></div>
}

function ClaimDetails({ claim, onBack, onEdit, onStatus }) {
  const approvedAmount = claim.approvedAmount == null || String(claim.approvedAmount).trim() === '' ? null : Number(claim.approvedAmount)
  const paidAmount = claim.paidAmount == null || String(claim.paidAmount).trim() === '' ? null : Number(claim.paidAmount)
  const hasApprovedAmount = approvedAmount !== null && Number.isFinite(approvedAmount)
  const hasPaidAmount = paidAmount !== null && Number.isFinite(paidAmount)
  const hasPaymentSummary = hasApprovedAmount && hasPaidAmount
  const relatedDataAvailability = claim.relatedDataAvailability || { payments: true, denial: true, tasks: true }
  const paymentProgress = hasPaymentSummary && approvedAmount > 0
    ? Math.min(100, Math.round(paidAmount / approvedAmount * 100))
    : 0

  return <>
    <button className="back-link" onClick={onBack}><ArrowLeft size={16} /> Back to Claims Management</button>
    <div className="detail-heading">
      <div><div className="overline">CLAIMS <span>•</span> CLAIM DETAILS</div><div className="detail-title-line"><h1>{claim.id}</h1><span className={statusClass(claim.status)}><i />{claim.status}</span></div><p className="subheading">Submitted {displayDate(claim.submittedDate)} <span className="bullet">·</span> {claim.claimType} claim</p></div>
      <div className="detail-actions"><button className="button button-outline" onClick={onEdit}><FileText size={16} /> Edit Claim</button><button className="button button-primary" onClick={onStatus}><Activity size={16} /> Update Status</button></div>
    </div>

    <div className="detail-layout">
      <div className="detail-main">
        <div className="detail-info-grid">
          <InfoCard title="Claim Information" icon={ClipboardList}>
            <DataPair label="Claim ID" value={claim.id} /><DataPair label="Claim type" value={claim.claimType} /><DataPair label="Provider" value={claim.provider} /><DataPair label="Service date" value={displayDate(claim.serviceDate)} /><DataPair label="Diagnosis / service" value={claim.diagnosis} /><DataPair label="Submitted date" value={displayDate(claim.submittedDate)} />
          </InfoCard>
          <InfoCard title="Customer Information" icon={UserRound}>
            <div className="detail-customer">{claim.initials && <div className="avatar avatar-detail">{claim.initials}</div>}<div><strong>{claim.customer || (claim.apiMode ? 'Customer name unavailable' : '—')}</strong><small>{claim.apiMode ? 'Not included in Claims API response' : 'Member since 2021'}</small></div></div>
            <DataPair label={claim.apiMode ? 'Patient reference' : 'Member ID'} value={claim.memberId} /><DataPair label="Email" value={claim.email} /><DataPair label="Phone" value={claim.phone} />
          </InfoCard>
          <InfoCard title="Insurance Information" icon={Shield}>
            <DataPair label="Insurance provider" value={claim.insurer} /><DataPair label="Policy number" value={claim.policyNumber} /><DataPair label="Assigned officer" value={claim.officer} /><DataPair label="Coverage" value={claim.coverage || (claim.apiMode ? 'Not provided by Claims API' : '—')} />
          </InfoCard>
          <InfoCard title="Financial Summary" icon={WalletCards} className="financial-card">
            <DataPair label="Claimed amount" value={money(claim.claimedAmount)} /><DataPair label="Approved amount" value={hasApprovedAmount ? money(approvedAmount) : 'Not available'} /><DataPair label="Paid amount" value={hasPaidAmount ? money(paidAmount) : claim.apiMode ? '—' : 'Not available'} /><DataPair label="Outstanding" value={hasPaymentSummary ? money(Math.max(0, approvedAmount - paidAmount)) : 'Not available'} />
            <div className="financial-progress"><div><span>Payment progress</span><strong>{hasPaymentSummary ? `${paymentProgress}%` : 'Not available'}</strong></div><i><b style={{ width: `${hasPaymentSummary ? paymentProgress : 0}%` }} /></i></div>
          </InfoCard>
        </div>

        <InfoCard title="Payment History" icon={WalletCards} className="history-card">
          {!relatedDataAvailability.payments ? <div className="inline-empty"><WalletCards size={17} /><span>Payment history is not provided by the Claims API.</span></div> : claim.paymentHistory?.length ? <div className="table-scroll"><table className="mini-table"><thead><tr><th>PAYMENT DATE</th><th>REFERENCE</th><th>METHOD</th><th>AMOUNT</th><th>STATUS</th></tr></thead><tbody>{claim.paymentHistory.map((payment) => <tr key={payment.reference}><td>{displayDate(payment.date)}</td><td className="mono">{payment.reference}</td><td>{payment.method}</td><td className="money-cell">{money(payment.amount)}</td><td><span className="badge badge-paid"><i />{payment.status}</span></td></tr>)}</tbody></table></div> : <div className="inline-empty"><WalletCards size={17} /><span>No payments have been recorded for this claim.</span></div>}
        </InfoCard>

        {!relatedDataAvailability.denial ? <InfoCard title="Related Denial Incident" icon={Shield}><div className="inline-empty"><Shield size={17} /><span>Denial incident information is not provided by the Claims API.</span></div></InfoCard> : claim.denial ? <InfoCard title="Related Denial Incident" icon={Shield} className="denial-card">
          <div className="denial-content"><div className="denial-symbol"><Shield size={18} /></div><div className="denial-copy"><div><strong>{claim.denial.category || `Denial ${claim.denial.code}`}</strong><span className="badge badge-denied"><i />Open</span></div><p>{claim.denial.reason}</p><small>Created {claim.denial.date} <span>·</span> Owner: {claim.denial.owner}</small></div></div>
        </InfoCard> : <InfoCard title="Related Denial Incident" icon={Shield}><div className="inline-empty"><Check size={17} /><span>No denial incidents linked to this claim.</span></div></InfoCard>}

        <InfoCard title="Related Follow-up Tasks" icon={ListChecks}>
          {!relatedDataAvailability.tasks ? <div className="inline-empty"><ListChecks size={17} /><span>Follow-up task information is not provided by the Claims API.</span></div> : claim.tasks?.length ? <div className="task-list">{claim.tasks.map((task, index) => <div className="task-row" key={`${task.title}-${index}`}><div className={`task-check ${task.status === 'Completed' ? 'task-done' : ''}`}>{task.status === 'Completed' && <Check size={12} />}</div><div className="task-info"><strong>{task.title}</strong><small>Assigned to {task.assignee} <span>·</span> Due {task.due}</small></div><div className="task-meta">{task.priority && <span className={`priority priority-${task.priority.toLowerCase()}`}>{task.priority}</span>}<span className={`task-status ${task.status === 'Completed' ? 'task-status-done' : ''}`}>{task.status}</span></div></div>)}</div> : <div className="inline-empty"><ListChecks size={17} /><span>No follow-up tasks have been assigned.</span></div>}
        </InfoCard>
      </div>

      <aside className="detail-aside">
        <InfoCard title="Status History" icon={Activity} className="timeline-card">
          {claim.history?.length ? <div className="timeline">{[...claim.history].map((event, index) => <div className={`timeline-event ${index === 0 ? 'timeline-current' : ''}`} key={`${event.title}-${event.date}-${index}`}><div className="timeline-marker">{index === 0 ? <i /> : <Check size={11} />}</div><div><strong>{event.title}</strong><p>{event.detail}</p><small>{event.date}</small></div></div>)}</div> : <div className="inline-empty"><Activity size={17} /><span>No status history available yet.</span></div>}
        </InfoCard>
        <section className="detail-help"><div className="help-icon"><FilePlus2 size={17} /></div><div><strong>Need to take action?</strong><p>Keep this claim moving with a follow-up or status update.</p><button onClick={onStatus}>Update claim status <ChevronRight size={14} /></button></div></section>
      </aside>
    </div>
    <p className="page-footnote">CLAIM RECORD <span>•</span> {claim.id} <span>•</span> {claim.apiMode ? 'LIVE API' : 'MOCK DATA'} ENVIRONMENT</p>
  </>
}

function PlaceholderPage({ title }) {
  return <section className="placeholder-view"><div className="placeholder-icon"><LayoutDashboard size={23} /></div><div className="overline">ADMIN CONSOLE <span>•</span> COMING SOON</div><h1>{title}</h1><p>This module is managed by another team member. The page will be available when that module is integrated.</p><div className="placeholder-tag"><Activity size={14} /> Module placeholder</div></section>
}

export default App
