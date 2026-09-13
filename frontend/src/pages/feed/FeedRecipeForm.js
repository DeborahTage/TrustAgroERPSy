import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { feedApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Button } from '../../components/ui/button';
import { Plus, Trash2, Calculator } from 'lucide-react';
import { toast } from 'react-toastify';
import { cn } from '../../lib/utils';

const FeedRecipeForm = () => {
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState({
    name: '',
    growthStage: 'Starter',
    proteinPercentage: 18,
    energyKcal: 3000,
    costPerKg: 0,
    ingredients: [{ ingredientName: '', percentage: 0 }],
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const totalPct = recipe.ingredients.reduce((sum, i) => sum + (Number(i.percentage) || 0), 0);

  const addIngredient = () => {
    setRecipe({ ...recipe, ingredients: [...recipe.ingredients, { ingredientName: '', percentage: 0 }] });
  };

  const removeIngredient = (index) => {
    setRecipe({ ...recipe, ingredients: recipe.ingredients.filter((_, i) => i !== index) });
  };

  const handleIngredientChange = (index, field, value) => {
    const next = [...recipe.ingredients];
    next[index][field] = field === 'percentage' ? parseFloat(value) || 0 : value;
    setRecipe({ ...recipe, ingredients: next });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (totalPct !== 100) {
      setError('Total ingredients must add up to 100%.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await feedApi.createRecipe(recipe);
      toast.success('Recipe formulated successfully');
      navigate('/feed/silos');
    } catch {
      setError('Formulation failed. Check inputs and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title="Feed Formulation"
      subtitle="Craft optimized feed recipes with precise nutritional balance and cost control"
      breadcrumbs={['Farm', 'Feed', 'Recipes']}
      backTo="/feed/silos"
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2 md:col-span-2">
            <Label>Recipe Name *</Label>
            <Input
              placeholder="Broiler Starter Plus"
              value={recipe.name}
              onChange={(e) => setRecipe({ ...recipe, name: e.target.value })}
              required
            />
          </div>
          <div className="space-y-2">
            <Label>Protein %</Label>
            <Input
              type="number"
              value={recipe.proteinPercentage}
              onChange={(e) => setRecipe({ ...recipe, proteinPercentage: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <Label>Energy (Kcal)</Label>
            <Input
              type="number"
              value={recipe.energyKcal}
              onChange={(e) => setRecipe({ ...recipe, energyKcal: e.target.value })}
            />
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <Label className="text-sm font-semibold">Composition Matrix</Label>
            <Button type="button" variant="outline" size="sm" onClick={addIngredient}>
              <Plus className="h-4 w-4 mr-1" /> Add Ingredient
            </Button>
          </div>
          <div className="space-y-3">
            {recipe.ingredients.map((ing, idx) => (
              <div key={idx} className="flex gap-3 items-end">
                <div className="flex-1">
                  <Input
                    placeholder="Maize, Soy Bean, etc."
                    value={ing.ingredientName}
                    onChange={(e) => handleIngredientChange(idx, 'ingredientName', e.target.value)}
                    required
                  />
                </div>
                <div className="w-28">
                  <Input
                    type="number"
                    placeholder="%"
                    value={ing.percentage}
                    onChange={(e) => handleIngredientChange(idx, 'percentage', e.target.value)}
                    required
                  />
                </div>
                {recipe.ingredients.length > 1 && (
                  <Button type="button" variant="ghost" size="icon" onClick={() => removeIngredient(idx)} className="text-rose-600">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="flex justify-between items-center pt-4 border-t">
          <div className={cn('flex items-center gap-2 font-semibold', totalPct === 100 ? 'text-emerald-600' : 'text-amber-600')}>
            <Calculator className="h-5 w-5" />
            Total: {totalPct}%
          </div>
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? 'Saving...' : 'Save Formulation'}
          </Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default FeedRecipeForm;
