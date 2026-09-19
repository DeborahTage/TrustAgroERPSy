import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { masterDataApi } from '../api';
import { useAuth } from './AuthContext';

const MasterDataContext = createContext();

export const MasterDataProvider = ({ children }) => {
    const { user, loading: authLoading, hasPermission } = useAuth();
    const [data, setData] = useState({});
    const [loading, setLoading] = useState(true);
    const canReadMasterData = hasPermission('masterdata:read');

    const fetchCategory = useCallback(async (category) => {
        try {
            const response = await masterDataApi.getByCategory(category);
            if (response.data?.success) {
                setData(prev => ({
                    ...prev,
                    [category]: response.data.data
                }));
            }
        } catch (error) {
            // Do not console.error(Error) — CRA overlays treat that as an uncaught runtime error.
            const status = error?.response?.status;
            const message = error?.response?.data?.message || error?.message || 'request failed';
            // eslint-disable-next-line no-console
            console.warn(`[master-data] ${category} failed (${status || 'network'}): ${message}`);
        }
    }, []);

    const loadAll = useCallback(async () => {
        setLoading(true);
        // Categories we need eagerly for forms
        const categories = [
            'BREED', 'DRUG_TYPE', 'FEED_TYPE', 'BARN_TYPE',
            'DISEASE_TYPE', 'VACCINE_TYPE', 'FLOCK_PURPOSE'
        ];

        await Promise.all(categories.map(c => fetchCategory(c)));
        setLoading(false);
    }, [fetchCategory]);

    useEffect(() => {
        if (!authLoading && user && canReadMasterData) {
            loadAll();
        } else if (!authLoading) {
            setLoading(false);
        }
    }, [user, authLoading, canReadMasterData, loadAll]);

    const refreshCategory = (category) => fetchCategory(category);

    return (
        <MasterDataContext.Provider value={{ data, loading, refreshCategory }}>
            {children}
        </MasterDataContext.Provider>
    );
};

// Hook that returns an array of { value, label } suitable for Select dropdowns
export const useMasterData = (category) => {
    const context = useContext(MasterDataContext);
    if (!context) {
        throw new Error('useMasterData must be used within a MasterDataProvider');
    }

    const items = context.data[category] || [];

    // Transform to standard select format
    const options = items.map(item => ({
        value: item.value,
        label: item.label,
        description: item.description
    }));

    return { options, loading: context.loading, refresh: () => context.refreshCategory(category) };
};
