import api from './axios'

export const getPoints = () => {
    return api.get('/points')
}

export const getPointUsagePlaces = (pointProviderId) => {
    return api.get(`/points/${pointProviderId}/usage-places`)
}