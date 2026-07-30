import api from './axios'

export const getMyMemberships = () => {
    return api.get('/memberships')
}

export const getMembershipProviders = () => {
    return api.get('/memberships/providers')
}

export const registerMembership = (pointProviderId) => {
    return api.post('/memberships', {
        pointProviderId,
    })
}

export const getMembershipDetail = (membershipRegisterId) => {
    return api.get(`/memberships/${membershipRegisterId}`)
}

export const cancelMembership = (membershipRegisterId) => {
    return api.delete(`/memberships/${membershipRegisterId}`)
}