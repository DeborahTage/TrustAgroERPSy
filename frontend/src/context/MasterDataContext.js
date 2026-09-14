import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { masterDataApi } from '../api';
import { useAuth } from './AuthContext';

const MasterDataContext = createContext();

export const MasterDataProvider = ({ children }) => {
    const { user, loading: authLoading } = useAuth();
    const [data, setData] = useState({});
    const [loading, setLoading] = useState(true);

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
            console.error(`Failed to fetch master data for ${category}:`, error);
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
        if (!authLoading && user) {
            loadAll();
        } else if (!authLoading && !user) {
            setLoading(false);
        }
    }, [user, authLoading, loadAll]);

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
