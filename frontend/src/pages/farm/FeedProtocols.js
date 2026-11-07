import React, { useState } from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Badge } from '../../components/ui/badge';
import { Printer, Calculator, Leaf, FlaskConical, Droplet } from 'lucide-react';
import { renderFeedProtocolGuideHtml, printHtml } from '../../lib/trustDocumentTemplates';

const FeedProtocols = () => {
  const [birdsCount, setBirdsCount] = useState(1000);
  const [dailyIntakeGrams, setDailyIntakeGrams] = useState(100);

  // Calculations
  const dailyFeedKg = (birdsCount * dailyIntakeGrams) / 1000;
  const bokashiDailyKg = dailyFeedKg * 0.04;
  const standardFeedDailyKg = dailyFeedKg * 0.96;

  // 21-day fermentation cycle
  const bokashi21DaysKg = bokashiDailyKg * 21;
  const bokashiBatches = Math.max(1, Math.ceil(bokashi21DaysKg / 40)); // 40kg bran base
  const emLiters = bokashiBatches * 2;
  const molassesKg = bokashiBatches * 2;
  const waterLiters = bokashiBatches * 16;
  const branKg = bokashiBatches * 40;

  // Water calculations
  const dailyWaterLiters = dailyFeedKg * 2; // avg 2:1 water:feed ratio
  const aceticAcidWeeklyMl = (dailyWaterLiters * 1) * 2; // 1ml/L twice a week
  const emWaterSanitizationLiters = (dailyWaterLiters / 300).toFixed(2);

  const handlePrint = () => {
    printHtml(renderFeedProtocolGuideHtml({ birdsCount, dailyFeedKg }), 'Feed Preparation Protocol Guide');
  };

  return (
    <EnterprisePage
      title="Feed Preparation Protocols & Formulation Guide"
      subtitle="Official Trust Agro fermentation recipes, probiotic supplementation, and water sanitation protocols"
      breadcrumbs={['Farm', 'Feed Protocols']}
      actions={
        <Button size="sm" variant="outline" onClick={handlePrint}>
          <Printer className="h-4 w-4 mr-1.5" /> Print Official Protocol
        </Button>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* Bokashi Section */}
        <Card className="border-emerald-500/30 bg-emerald-500/5">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base text-emerald-800 dark:text-emerald-400 flex items-center gap-2">
                <Leaf className="h-4 w-4" /> 1. Bokashi Protocol
              </CardTitle>
              <Badge className="bg-emerald-600">4% of Daily Diet</Badge>
            </div>
            <CardDescription className="text-xs">
              Anaerobic probiotic fermentation for optimal intestinal health & feed conversion ratio
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <div className="p-3 bg-card rounded-lg border border-border/60">
              <div className="font-semibold text-foreground mb-1">Standard Batch Formula:</div>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li><strong>2 Liters</strong> Effective Microorganisms (EM)</li>
                <li><strong>2 Liters / kg</strong> Cane Molasses (ሞላሰስ)</li>
                <li><strong>16 Liters</strong> Clean Well Water (un-chlorinated)</li>
                <li><strong>40 kg</strong> Wheat Bran (ፋጉሎ / መኖ)</li>
              </ul>
            </div>
            <div className="text-muted-foreground">
              <strong>Fermentation:</strong> Pack tightly in airtight barrels for <strong>21 days</strong>.
              Store in shade between 20°C–30°C. Feed <strong>4% Bokashi</strong> thoroughly blended with <strong>96% commercial feed</strong> daily.
            </div>
          </CardContent>
        </Card>

        {/* Acetic Acid Section */}
        <Card className="border-amber-500/30 bg-amber-500/5">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base text-amber-800 dark:text-amber-400 flex items-center gap-2">
                <FlaskConical className="h-4 w-4" /> 2. Acetic Acid Protocol
              </CardTitle>
              <Badge className="bg-amber-600">1ml / 1L Water</Badge>
            </div>
            <CardDescription className="text-xs">
              Gut acidification to suppress Salmonella and E. coli during high ambient temperatures
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <div className="p-3 bg-card rounded-lg border border-border/60">
              <div className="font-semibold text-foreground mb-1">Application Schedule:</div>
              <p className="text-muted-foreground">
                Apply <strong>1ml per 1 Liter</strong> of drinking water <strong>twice weekly</strong> (~<strong>{aceticAcidWeeklyMl.toFixed(0)} ml/wk</strong> for current flock) during the hot dry season.
              </p>
            </div>
            <div className="p-3 bg-amber-500/10 rounded-lg text-amber-800 dark:text-amber-300">
              <strong>Note:</strong> Never combine organic acids with live vaccines or water-administered antibiotics simultaneously.
            </div>
          </CardContent>
        </Card>

        {/* EM Water & Greens Section */}
        <Card className="border-blue-500/30 bg-blue-500/5">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base text-blue-800 dark:text-blue-400 flex items-center gap-2">
                <Droplet className="h-4 w-4" /> 3. EM & Fresh Greens
              </CardTitle>
              <Badge className="bg-blue-600">1L / 300L Water</Badge>
            </div>
            <CardDescription className="text-xs">
              Natural microbial water hygiene and natural vitamin supplementation
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <div className="p-3 bg-card rounded-lg border border-border/60">
              <div className="font-semibold text-foreground mb-1">EM Water Sanitization:</div>
              <p className="text-muted-foreground">
                Mix <strong>1 Liter EM per 300 Liters</strong> well water (~<strong>{emWaterSanitizationLiters} L/day</strong> for current flock). Enhances palatability and suppresses pathogens in drinker lines.
              </p>
            </div>
            <div className="p-3 bg-card rounded-lg border border-border/60">
              <div className="font-semibold text-foreground mb-1">Approved Fresh Greens:</div>
              <div className="flex flex-wrap gap-1.5 mt-1">
                <Badge variant="outline" className="border-emerald-500 text-emerald-700">Alfalfa (አልፋልፋ)</Badge>
                <Badge variant="outline" className="border-emerald-500 text-emerald-700">Espaniya (እስፓንያ)</Badge>
                <Badge variant="outline" className="border-emerald-500 text-emerald-700">Kale (ጎመን)</Badge>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Interactive Formulation Calculator */}
      <Card className="border-border/70 shadow-sm mb-6">
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Calculator className="h-5 w-5 text-primary" /> Interactive Batch Recipe Calculator
          </CardTitle>
          <CardDescription>
            Enter your flock census and daily intake to automatically compute exact daily rations and 21-day fermentation batches
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
            <div className="space-y-2">
              <Label>Flock Bird Population</Label>
              <Input
                type="number"
                min="10"
                step="50"
                value={birdsCount}
                onChange={(e) => setBirdsCount(Math.max(1, Number(e.target.value)))}
              />
              <span className="text-xs text-muted-foreground">Total birds in house or barn</span>
            </div>
            <div className="space-y-2">
              <Label>Avg Daily Intake per Bird (grams)</Label>
              <Input
                type="number"
                min="30"
                max="150"
                value={dailyIntakeGrams}
                onChange={(e) => setDailyIntakeGrams(Math.max(1, Number(e.target.value)))}
              />
              <span className="text-xs text-muted-foreground">e.g. 70g (pullet developer) to 115g (peak layer)</span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="p-4 rounded-xl bg-card border border-border/70 text-center">
              <span className="text-xs text-muted-foreground block">Total Daily Feed</span>
              <strong className="text-xl text-foreground font-mono">{dailyFeedKg.toFixed(1)} kg</strong>
            </div>
            <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-center">
              <span className="text-xs text-emerald-800 dark:text-emerald-300 block">Daily Bokashi (4%)</span>
              <strong className="text-xl text-emerald-700 dark:text-emerald-400 font-mono">{bokashiDailyKg.toFixed(2)} kg</strong>
            </div>
            <div className="p-4 rounded-xl bg-card border border-border/70 text-center">
              <span className="text-xs text-muted-foreground block">Daily Base Feed (96%)</span>
              <strong className="text-xl text-foreground font-mono">{standardFeedDailyKg.toFixed(1)} kg</strong>
            </div>
            <div className="p-4 rounded-xl bg-primary/10 border border-primary/30 text-center">
              <span className="text-xs text-primary block">21-Day Bokashi Batches</span>
              <strong className="text-xl text-primary font-mono">{bokashiBatches} units ({bokashi21DaysKg.toFixed(0)} kg)</strong>
            </div>
          </div>

          <div className="overflow-x-auto rounded-xl border border-border/70">
            <table className="w-full text-sm">
              <thead className="erp-table-head">
                <tr>
                  <th className="p-3 text-left">Fermentation Raw Material</th>
                  <th className="p-3 text-left">Base Unit (per 40kg bran)</th>
                  <th className="p-3 text-right">Required for {bokashiBatches} Batch(es) (21 Days)</th>
                </tr>
              </thead>
              <tbody className="bg-card">
                <tr className="border-t border-border/60">
                  <td className="p-3 font-semibold">Effective Microorganisms (EM)</td>
                  <td className="p-3 text-muted-foreground">2 Liters</td>
                  <td className="p-3 text-right font-bold text-emerald-700">{emLiters} Liters</td>
                </tr>
                <tr className="border-t border-border/60">
                  <td className="p-3 font-semibold">Cane Molasses (ሞላሰስ)</td>
                  <td className="p-3 text-muted-foreground">2 kg / Liters</td>
                  <td className="p-3 text-right font-bold text-amber-700">{molassesKg} kg</td>
                </tr>
                <tr className="border-t border-border/60">
                  <td className="p-3 font-semibold">Clean Unchlorinated Well Water</td>
                  <td className="p-3 text-muted-foreground">16 Liters</td>
                  <td className="p-3 text-right font-bold text-blue-700">{waterLiters} Liters</td>
                </tr>
                <tr className="border-t border-border/60">
                  <td className="p-3 font-semibold">Wheat Bran (ፋጉሎ / መኖ)</td>
                  <td className="p-3 text-muted-foreground">40 kg</td>
                  <td className="p-3 text-right font-bold text-foreground">{branKg} kg</td>
                </tr>
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default FeedProtocols;
